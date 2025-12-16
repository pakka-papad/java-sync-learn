package org.example.datastructures.queue;

import java.util.NoSuchElementException;

public interface Queue<E> {

    void add(E item);

    E poll() throws NoSuchElementException;

    E peek() throws NoSuchElementException;

    boolean remove(E item);

    int size();
}
