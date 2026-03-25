package org.example.boundedbuffer;

import java.util.ArrayList;
import java.util.List;

public class CircularBoundedBufferV1<T> implements BlockingBuffer<T> {

    private final Object[] buffer;
    private int takeIndex;
    private int putIndex;
    private int count;

    CircularBoundedBufferV1(int capacity) {
        buffer = new Object[capacity];
        takeIndex = 0;
        putIndex = 0;
        count = 0;
    }

    @Override
    public synchronized void produce(T item) throws InterruptedException {
        while (count == buffer.length) {
            wait();
        }
        buffer[putIndex] = item;
        putIndex = (putIndex + 1) % buffer.length;
        count++;
        notifyAll();
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized T consume() throws InterruptedException {
        while (count == 0) {
            wait();
        }
        var res = (T) buffer[takeIndex];
        buffer[takeIndex] = null;
        takeIndex = (takeIndex + 1) % buffer.length;
        count--;
        notifyAll();
        return res;
    }

    @Override
    public synchronized int size() {
        return count;
    }
}
