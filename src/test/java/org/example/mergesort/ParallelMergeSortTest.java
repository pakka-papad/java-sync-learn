package org.example.mergesort;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class ParallelMergeSortTest {

    @Test
    void testSmallArray() {
        Integer[] arr = {5, 2, 9, 1, 5, 6};
        Integer[] expected = {1, 2, 5, 5, 6, 9};
        ParallelMergeSort.sort(arr);
        assertArrayEquals(expected, arr);
    }

    @Test
    void testEmptyArray() {
        Integer[] arr = {};
        ParallelMergeSort.sort(arr);
        assertArrayEquals(new Integer[]{}, arr);
    }

    @Test
    void testNullArray() {
        ParallelMergeSort.sort(null);
    }

    @Test
    void testSortedArray() {
        Integer[] arr = {1, 2, 3, 4, 5};
        Integer[] expected = {1, 2, 3, 4, 5};
        ParallelMergeSort.sort(arr);
        assertArrayEquals(expected, arr);
    }

    @Test
    void testReverseSortedArray() {
        Integer[] arr = {5, 4, 3, 2, 1};
        Integer[] expected = {1, 2, 3, 4, 5};
        ParallelMergeSort.sort(arr);
        assertArrayEquals(expected, arr);
    }

    @Test
    void testLargeArray() {
        int size = 1000;
        Integer[] arr = new Integer[size];
        Integer[] expected = new Integer[size];
        Random rand = new Random(42);
        for (int i = 0; i < size; i++) {
            int val = rand.nextInt(10000);
            arr[i] = val;
            expected[i] = val;
        }
        Arrays.sort(expected);
        ParallelMergeSort.sort(arr);
        assertArrayEquals(expected, arr);
    }

    @Test
    void testSingleElement() {
        Integer[] arr = {42};
        ParallelMergeSort.sort(arr);
        assertArrayEquals(new Integer[]{42}, arr);
    }
}
