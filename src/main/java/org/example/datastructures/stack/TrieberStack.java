package org.example.datastructures.stack;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TrieberStack<E> implements Stack<E> {

    private static class LNode<E> {
        private final E item;
        private LNode<E> next;

        LNode(E item, LNode<E> next) {
            this.item = item;
            this.next = next;
        }
    }

    private final AtomicReference<LNode<E>> head;
    private final AtomicInteger size;

    TrieberStack() {
        head = new AtomicReference<>(null);
        size = new AtomicInteger(0);
    }

    @Override
    public void push(E item) {
        final var toSet = new LNode<>(item, null);
        do {
            toSet.next = head.get();
        } while (!head.compareAndSet(toSet.next, toSet));
        size.incrementAndGet();
    }

    @Override
    public E pop() throws NoSuchElementException {
        LNode<E> currHead = null;
        do {
            currHead = head.get();
            if (currHead == null) {
                throw new NoSuchElementException();
            }
        } while (!head.compareAndSet(currHead, currHead.next));
        size.decrementAndGet();
        return currHead.item;
    }

    @Override
    public E peek() throws NoSuchElementException {
        var currHead = head.get();
        if (currHead == null) {
            throw new NoSuchElementException();
        }
        return currHead.item;
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public boolean isEmpty() {
        return (size.get() <= 0);
    }
}
