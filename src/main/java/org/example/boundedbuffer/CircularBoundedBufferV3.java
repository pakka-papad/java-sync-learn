package org.example.boundedbuffer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CircularBoundedBufferV3<T> implements BlockingBuffer<T> {

    private final Object[] buffer;
    private int putIndex;
    private int takeIndex;
    private final AtomicInteger count;
    private final Lock putLock;
    private final Condition putWait;
    private final Lock takeLock;
    private final Condition takeWait;

    CircularBoundedBufferV3(int capacity) {
        assert capacity > 0;
        buffer = new Object[capacity];
        putIndex = 0;
        takeIndex = 0;
        count = new AtomicInteger(0);
        putLock = new ReentrantLock();
        putWait = putLock.newCondition();
        takeLock = new ReentrantLock();
        takeWait = takeLock.newCondition();
    }

    @Override
    public void produce(T item) throws InterruptedException {
        final int prevSize;
        putLock.lock();
        try {
            while (count.get() == buffer.length) {
                putWait.await();
            }
            buffer[putIndex] = item;
            putIndex = (putIndex + 1) % buffer.length;
            prevSize = count.getAndIncrement();
            if (prevSize + 1 < buffer.length) {
                putWait.signal();
            }
        } finally {
            putLock.unlock();
        }
        if (prevSize == 0) {
            takeLock.lock();
            try {
                takeWait.signal();
            } finally {
                takeLock.unlock();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T consume() throws InterruptedException {
        T retVal = null;
        takeLock.lock();
        final int prevSize;
        try {
            while (count.get() == 0) {
                takeWait.await();
            }
            retVal = (T) buffer[takeIndex];
            buffer[takeIndex] = null;
            takeIndex = (takeIndex + 1) % buffer.length;
            prevSize = count.getAndDecrement();
            if (prevSize - 1 > 0) {
                takeWait.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (prevSize == buffer.length) {
            putLock.lock();
            try {
                putWait.signal();
            } finally {
                putLock.unlock();
            }
        }
        return retVal;
    }

    @Override
    public int size() {
        return count.get();
    }
}
