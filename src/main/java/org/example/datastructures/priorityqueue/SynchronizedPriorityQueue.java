package org.example.datastructures.priorityqueue;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class SynchronizedPriorityQueue<E extends Comparable<E>> implements PriorityQueue<E> {

    private Object[] heap;
    private volatile int addAt;

    SynchronizedPriorityQueue(int capacity) {
        capacity = Math.max(capacity, 16);
        if (Integer.bitCount(capacity) != 1) {
            capacity = (1 << (32 - Integer.numberOfLeadingZeros(capacity)));
        }
        heap = new Object[capacity];
        this.addAt = 0;
    }

    @Override
    public synchronized void add(E item) {
        if (item == null) {
            throw new NullPointerException();
        }
        if (addAt == heap.length) {
            reAllocate();
        }
        heap[addAt] = item;
        bubbleUp(addAt);
        addAt++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized E removeTop() throws NoSuchElementException {
        if (addAt == 0) {
            throw new NoSuchElementException();
        }
        var retVal = heap[0];
        heap[0] = heap[addAt - 1];
        heap[addAt - 1] = null;
        addAt--;
        bubbleDown(0, addAt);
        return (E) retVal;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized E peekTop() throws NoSuchElementException {
        if (addAt == 0) {
            throw new NoSuchElementException();
        }
        return (E) heap[0];
    }

    @Override
    public int size() {
        return addAt;
    }

    void reAllocate() {
        int newSize = (heap.length << 1);
        heap = Arrays.copyOf(heap, newSize);
    }

    @SuppressWarnings("unchecked")
    void bubbleUp(int position) {
        var currPos = position;
        while (currPos != 0) {
            int parentPos = (currPos - 1) / 2;
            var res = ((Comparable<E>) heap[currPos]).compareTo((E) heap[parentPos]);
            if (res < 0) {
                Object temp = heap[currPos];
                heap[currPos] = heap[parentPos];
                heap[parentPos] = temp;
                currPos = parentPos;
                continue;
            }
            break;
        }
    }

    @SuppressWarnings("unchecked")
    void bubbleDown(int position, int heapSize) {
        int currPos = position;
        while (currPos < heapSize) {
            int lIdx = 2 * currPos + 1;
            int rIdx = lIdx + 1;
            if (lIdx >= heapSize) {
                break;
            }
            int smallerIdx = lIdx;
            if (rIdx < heapSize && ((Comparable<E>) heap[rIdx]).compareTo((E) heap[lIdx]) < 0) {
                smallerIdx = rIdx;
            }
            if (((Comparable<E>) heap[currPos]).compareTo((E) heap[smallerIdx]) > 0) {
                var temp = heap[smallerIdx];
                heap[smallerIdx] = heap[currPos];
                heap[currPos] = temp;
                currPos = smallerIdx;
            } else {
                break;
            }
        }
    }
}
