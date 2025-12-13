package org.example.datastructures.stack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SafeStack<E> implements Stack<E> {
    private final ReadWriteLock lock;
    private final Deque<E> list;
    private volatile int size;

    SafeStack() {
        lock = new ReentrantReadWriteLock();
        list = new ArrayDeque<>();
    }

    @Override
    public void push(E item) {
        var writeLock = lock.writeLock();
        writeLock.lock();
        try {
            list.addLast(item);
            size++;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E pop() throws NoSuchElementException {
        var writeLock = lock.writeLock();
        writeLock.lock();
        try {
            var item = list.removeLast();
            size--;
            return item;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E peek() throws NoSuchElementException {
        var readLock = lock.readLock();
        readLock.lock();
        try {
            return list.getLast();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return (size <= 0);
    }
}
