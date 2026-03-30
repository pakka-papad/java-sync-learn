package org.example.mergesort;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;

public class MergeSortPerformanceTest {

    private static final int SIZE = 1_000_000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int MEASURE_ITERATIONS = 10;

    @Test
    void runComparison() {
        System.out.println("--- Starting Merge Sort Performance Comparison ---");
        System.out.println("Array size: " + SIZE);

        // Warm up the JVM to trigger JIT optimizations
        warmUp();

        long parallelTotal = 0;
        long sequentialTotal = 0;
        long arraysSortTotal = 0;

        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            Integer[] baseArr = generateRandomArray(SIZE);

            // Parallel
            Integer[] parallelArr = Arrays.copyOf(baseArr, baseArr.length);
            parallelTotal += measureTime(() -> ParallelMergeSort.sort(parallelArr));

            // Sequential
            Integer[] sequentialArr = Arrays.copyOf(baseArr, baseArr.length);
            sequentialTotal += measureTime(() -> SequentialMergeSort.sort(sequentialArr));

            // Arrays.sort (Standard)
            Integer[] standardArr = Arrays.copyOf(baseArr, baseArr.length);
            arraysSortTotal += measureTime(() -> Arrays.sort(standardArr));
        }

        printResults("Parallel", parallelTotal / MEASURE_ITERATIONS);
        printResults("Sequential", sequentialTotal / MEASURE_ITERATIONS);
        printResults("Arrays.sort", arraysSortTotal / MEASURE_ITERATIONS);
    }

    private void warmUp() {
        System.out.println("Warming up JVM...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Integer[] arr = generateRandomArray(100_000);
            ParallelMergeSort.sort(Arrays.copyOf(arr, arr.length));
            SequentialMergeSort.sort(Arrays.copyOf(arr, arr.length));
            Arrays.sort(Arrays.copyOf(arr, arr.length));
        }
    }

    private long measureTime(Runnable task) {
        long start = System.nanoTime();
        task.run();
        return System.nanoTime() - start;
    }

    private Integer[] generateRandomArray(int size) {
        Random rand = new Random();
        Integer[] arr = new Integer[size];
        for (int i = 0; i < size; i++) {
            arr[i] = rand.nextInt(size);
        }
        return arr;
    }

    private void printResults(String label, long avgNano) {
        System.out.printf("%s: avg %d ms%n", label, avgNano / 1_000_000);
    }
}
