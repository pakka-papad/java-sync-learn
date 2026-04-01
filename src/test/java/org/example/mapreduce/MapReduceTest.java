package org.example.mapreduce;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class MapReduceTest {

    @Test
    void testSumIntegers() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Integer result = MapReduce.run(numbers, x -> x, Integer::sum, 0);
        assertEquals(55, result);
    }

    @Test
    void testStringLengthSum() {
        List<String> words = Arrays.asList("apple", "banana", "cherry");
        Integer result = MapReduce.run(words, String::length, Integer::sum, 0);
        assertEquals(17, result);
    }

    @Test
    void testEmptySource() {
        List<Integer> emptyList = Collections.emptyList();
        Integer result = MapReduce.run(emptyList, x -> x, Integer::sum, 42);
        assertEquals(42, result);
    }

    @Test
    void testNullSource() {
        Integer result = MapReduce.run(null, (Integer x) -> x, Integer::sum, 0);
        assertEquals(0, result);
    }

    @Test
    void testLargeListSum() {
        int size = 100_000;
        Integer[] arr = new Integer[size];
        long expectedSum = 0;
        for (int i = 0; i < size; i++) {
            arr[i] = i;
            expectedSum += i;
        }
        List<Integer> numbers = Arrays.asList(arr);
        Long result = MapReduce.run(numbers, Integer::longValue, Long::sum, 0L);
        assertEquals(expectedSum, result);
    }

    @Test
    void testProductWithIdentity() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4);
        // (1 * 1) * 2 * 3 * 4 = 24
        Integer result = MapReduce.run(numbers, x -> x, (a, b) -> a * b, 1);
        assertEquals(24, result);
    }
}
