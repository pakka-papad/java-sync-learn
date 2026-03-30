package org.example.mergesort;

import java.lang.reflect.Array;

public class SequentialMergeSort {

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> void sort(T[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        T[] aux = (T[]) Array.newInstance(arr.getClass().getComponentType(), arr.length);
        sort(arr, 0, arr.length, aux);
    }

    private static <T extends Comparable<T>> void sort(T[] arr, int start, int end, T[] aux) {
        if (end - start <= 1) {
            return;
        }

        int mid = start + (end - start) / 2;
        sort(arr, start, mid, aux);
        sort(arr, mid, end, aux);

        merge(arr, start, mid, end, aux);
    }

    private static <T extends Comparable<T>> void merge(T[] arr, int start, int mid, int end, T[] aux) {
        System.arraycopy(arr, start, aux, start, end - start);

        int t = start;
        int p1 = start, p2 = mid;
        while (p1 < mid && p2 < end) {
            if (aux[p1].compareTo(aux[p2]) <= 0) {
                arr[t] = arr[p1];
                p1++;
            } else {
                arr[t] = arr[p2];
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
