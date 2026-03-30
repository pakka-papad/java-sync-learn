package org.example.mergesort;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ParallelMergeSort {

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> void sort(T[] arr) {
        if (arr == null || arr.length == 0) {
            return;
        }
        T[] aux = (T[]) Array.newInstance(arr.getClass().getComponentType(), arr.length);
        final var task = new MergeSortWork<T>(arr, 0, arr.length, aux);
        ForkJoinPool.commonPool().invoke(task);
    }

    private static final int LINEAR_THRESHOLD = 8;

    private static class MergeSortWork<T extends Comparable<T>> extends RecursiveAction {

        private final T[] arr;
        private final int start;
        private final int end;

        private final T[] aux;

        MergeSortWork(T[] arr, int start, int end, T[] aux) {
            this.arr = arr;
            this.start = start;
            this.end = end;
            this.aux = aux;
        }

        @Override
        protected void compute() {
            if (arr == null || arr.length == 0 || start >= end) {
                return;
            }
            final var len = end - start;
            if (len <= LINEAR_THRESHOLD) {
                Arrays.sort(arr, start, end);
                return;
            }
            final var mid = start + len / 2;
            final var task1 = new MergeSortWork<>(arr, start, mid, aux);
            final var task2 = new MergeSortWork<>(arr, mid, end, aux);
            invokeAll(task1, task2);

            System.arraycopy(arr, start, aux, start, end - start);

            int t = start;
            int p1 = start, p2 = mid;
            while (p1 < mid && p2 < end) {
                if (aux[p1].compareTo(aux[p2]) <= 0) {
                    arr[t] = aux[p1];
                    p1++;
                } else {
                    arr[t] = aux[p2];
                    p2++;
                }
                t++;
            }
            while (p1 < mid) {
                arr[t] = aux[p1];
                p1++;
                t++;
            }
            while (p2 < end) {
                arr[t] = aux[p2];
                p2++;
                t++;
            }
        }
    }
}
