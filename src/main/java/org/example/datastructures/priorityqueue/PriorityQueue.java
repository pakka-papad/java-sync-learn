package org.example.datastructures.priorityqueue;

import java.util.NoSuchElementException;

public interface PriorityQueue<E> {

    void add(E item);

    E removeTop() throws NoSuchElementException;

    E peekTop() throws NoSuchElementException;

    int size();
}
