package org.example.boundedbuffer;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LinkedBoundedBuffer<T> implements BlockingBuffer<T> {

    private final LinkedList<T> queue;
    private final Lock lock;
    private final Condition notEmpty;
    private final Condition notFull;

    private final int capacity;

    LinkedBoundedBuffer(int capacity) {
        this.capacity = capacity;
        queue = new LinkedList<>();
        lock = new ReentrantLock();
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();
    }

    @Override
    public void produce(T item) throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();
            }
            queue.addLast(item);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T consume() throws InterruptedException {
        lock.lock();
        try {
            while (queue.size() == 0) {
                notEmpty.await();
            }
            var res = queue.pollFirst();
            notFull.signal();
            return res;
        } finally {
            lock.unlock();
        }
    }
}
