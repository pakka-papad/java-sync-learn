package org.example.datastructures.stack;

import java.util.NoSuchElementException;

public interface Stack<E> {

    void push(E item);

    E pop() throws NoSuchElementException;

    E peek() throws NoSuchElementException;

    int size();

    boolean isEmpty();
}
