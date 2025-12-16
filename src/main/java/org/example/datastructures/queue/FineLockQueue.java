package org.example.datastructures.queue;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FineLockQueue<E> implements Queue<E> {

    private LNode<E> head;
    private LNode<E> tail;
    private final Lock headLock;
    private final Lock tailLock;
    private final AtomicInteger size;

    FineLockQueue() {
        var dummy = new LNode<E>(null, null);
        head = dummy;
        tail = dummy;
        headLock = new ReentrantLock();
        tailLock = new ReentrantLock();
        size = new AtomicInteger(0);
    }

    @Override
    public void add(E item) {
        var node = new LNode<>(item, null);
        tailLock.lock();
        try {
            tail.next = node;
            tail = node;
        } finally {
            tailLock.unlock();
        }
        size.incrementAndGet();
    }

    @Override
    public E poll() throws NoSuchElementException {
        E retVal;
        headLock.lock();
        try {
            if (head.next == null) {
                throw new NoSuchElementException();
            }
            var headNext = head.next;
            retVal = headNext.item;
            headNext.item = null;
            head = headNext;
        } finally {
            headLock.unlock();
        }
        size.decrementAndGet();
        return retVal;
    }

    @Override
    public E peek() throws NoSuchElementException {
        headLock.lock();
        try {
            if (head.next == null) {
                throw new NoSuchElementException();
            }
            return head.next.item;
        } finally {
            headLock.unlock();
        }
    }

    @Override
    public boolean remove(E item) {
        boolean removed = false;
        headLock.lock();
        try {
            tailLock.lock();
            try {
                var prev = findNode(item);
                if (prev != null) {
                    unlink(prev, prev.next);
                    removed = true;
                }
            } finally {
                tailLock.unlock();
            }
        } finally {
            headLock.unlock();
        }
        if (removed) {
            size.decrementAndGet();
        }
        return removed;
    }

    @Override
    public int size() {
        return size.get();
    }

    protected void unlink(LNode<E> prev, LNode<E> node) {
        prev.next = node.next;
        if (node == tail) {
            tail = prev;
        }
        node.item = null;
    }

    protected LNode<E> findNode(E item) {
        var prev = head;
        var it = head.next;
        while (it != null) {
            if (Objects.equals(it.item, item)) {
                return prev;
            }
            prev = it;
            it = it.next;
        }
        return null;
    }

    private static class LNode<E> {
        E item;
        LNode<E> next;

        private LNode(E item, LNode<E> next) {
            this.item = item;
            this.next = next;
        }
    }
}
