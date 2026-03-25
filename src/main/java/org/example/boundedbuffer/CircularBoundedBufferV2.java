package org.example.boundedbuffer;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CircularBoundedBufferV2<T> implements BlockingBuffer<T> {

    private final Object[] buffer;
    private int takeIndex;
    private int putIndex;
    private int count;
    private final Lock lock;
    private final Condition notEmpty;
    private final Condition notFull;

    CircularBoundedBufferV2(int capacity) {
        buffer = new Object[capacity];
        takeIndex = 0;
        putIndex = 0;
        count = 0;
        lock = new ReentrantLock();
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();
    }

    @Override
    public void produce(T item) throws InterruptedException {
        lock.lock();
        try {
            while (count == buffer.length) {
                notFull.await();
            }
            final var prevSize = count;
            buffer[putIndex] = item;
            putIndex = (putIndex + 1) % buffer.length;
            count++;
            if (prevSize == 0) {
                notEmpty.signal();
            }
            if (count < buffer.length) {
                notFull.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T consume() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await();
            }
            final var prevSize = count;
            var res = (T) buffer[takeIndex];
            buffer[takeIndex] = null;
            takeIndex = (takeIndex + 1) % buffer.length;
            count--;
            if (prevSize == buffer.length) {
                notFull.signal();
            }
            if (count > 0) {
                notEmpty.signal();
            }
            return res;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
