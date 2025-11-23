package org.example.boundedbuffer;

import java.util.ArrayList;
import java.util.List;

public class CircularBoundedBuffer<T> implements BlockingBuffer<T> {

    private final List<T> buffer;
    private int takeIndex;
    private int putIndex;
    private int count;

    CircularBoundedBuffer(int capacity) {
        buffer = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            buffer.add(null);
        }
        takeIndex = 0;
        putIndex = 0;
        count = 0;
    }

    @Override
    public synchronized void produce(T item) throws InterruptedException {
        while (count == buffer.size()) {
            wait();
        }
        buffer.set(putIndex, item);
        putIndex = (putIndex + 1) % buffer.size();
        count++;
        notifyAll();
    }

    @Override
    public synchronized T consume() throws InterruptedException {
        while (count == 0) {
            wait();
        }
        var res = buffer.get(takeIndex);
        buffer.set(takeIndex, null);
        takeIndex = (takeIndex + 1) % buffer.size();
        count--;
        notifyAll();
        return res;
    }
}
