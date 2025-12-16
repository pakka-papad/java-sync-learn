package org.example.datastructures.queue;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CoarseLockQueue<E> implements Queue<E> {

    private final ReadWriteLock lock;
    private LNode<E> head;
    private LNode<E> tail;
    private volatile int size;

    public CoarseLockQueue() {
        this.lock = new ReentrantReadWriteLock();
        this.size = 0;
    }

    @Override
    public void add(E item) {
        var writeLock = lock.writeLock();
        writeLock.lock();
        try {
            var node = new LNode<>(item, tail, null);
            if (tail == null) {
                head = node;
            } else {
                tail.next = node;
            }
            tail = node;
            size++;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E poll() throws NoSuchElementException {
        var writeLock = lock.writeLock();
        writeLock.lock();
        try {
            if (size == 0) {
                throw new NoSuchElementException();
            }
            var retVal = head.item;
            unlinkNode(head);
            size--;
            return retVal;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E peek() throws NoSuchElementException {
        var readLock = lock.readLock();
        readLock.lock();
        try {
            if (size == 0) {
                throw new NoSuchElementException();
            }
            return head.item;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean remove(E item) {
        var writeLock = lock.writeLock();
        writeLock.lock();
        try {
            if (size == 0) {
                return false;
            }
            var node = findNode(item);
            if (node == null) {
                return false;
            }
            unlinkNode(node);
            size--;
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public int size() {
        return size;
    }

    protected LNode<E> findNode(E item) {
        var it = head;
        while (it != null) {
            if (Objects.equals(it.item, item)) {
                return it;
            }
            it = it.next;
        }
        return null;
    }

    protected void unlinkNode(LNode<E> node) {
        var prev = node.prev;
        var next = node.next;
        if (prev != null) {
            prev.next = next;
        } else {
            head = next;
        }
        if (next != null) {
            next.prev = prev;
        } else {
            tail = prev;
        }
    }

    private static class LNode<E> {
        final E item;
        LNode<E> prev;
        LNode<E> next;

        private LNode(E item, LNode<E> prev, LNode<E> next) {
            this.item = item;
            this.prev = prev;
            this.next = next;
        }
    }
}
