package org.example.mapreduce;

import java.util.Spliterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

public class MapReduce<T,R> {

    public static <T,R> R run(Iterable<T> source, Function<T,R> mapper, BinaryOperator<R> reducer, R identity) {
        if (source == null) {
            return identity;
        }
        final var task = new MapReduceTask<>(source.spliterator(), mapper, reducer);
        var res = ForkJoinPool.commonPool().invoke(task);
        if (res == null) {
            return identity;
        } else {
            return reducer.apply(identity, res);
        }
    }

    private static class MapReduceTask<T,R> extends RecursiveTask<R> {

        private static final long SEQ_THRESHOLD = 1024;

        private final Spliterator<T> source;
        private final Function<T,R> mapper;
        private final BinaryOperator<R> reducer;

        MapReduceTask(Spliterator<T> source, Function<T,R> mapper, BinaryOperator<R> reducer) {
            this.source = source;
            this.mapper = mapper;
            this.reducer = reducer;
        }

        @Override
        protected R compute() {
            if (source == null) {
                return null;
            }

            if (source.estimateSize() <= SEQ_THRESHOLD) {
                return computeSequential();
            }

            final var right = source.trySplit();
            if (right == null) {
                return computeSequential();
            }

            final var task1 = new MapReduceTask<>(source, mapper, reducer);
            final var task2 = new MapReduceTask<>(right, mapper, reducer);
            invokeAll(task1, task2);

            var result = task1.join();
            final var res2 = task2.join();
            if (result == null) {
                result = res2;
            } else if (res2 != null) {
                result = reducer.apply(result, res2);
            }
            return result;
        }

        private R computeSequential() {
            final var consumer = new MapReduceConsumer<>(mapper, reducer);
            source.forEachRemaining(consumer);
            return consumer.getResult();
        }
    }

    private static class MapReduceConsumer<T,R> implements Consumer<T> {

        private final Function<T,R> mapper;
        private final BinaryOperator<R> reducer;
        private R result;

        private MapReduceConsumer(Function<T, R> mapper, BinaryOperator<R> reducer) {
            this.mapper = mapper;
            this.reducer = reducer;
            this.result = null;
        }

        @Override
        public void accept(T t) {
            final var mapped = mapper.apply(t);
            if (result == null) {
                result = mapped;
            } else if (mapped != null) {
                result = reducer.apply(result, mapped);
            }
        }

        public R getResult() {
            return result;
        }
    }
}
