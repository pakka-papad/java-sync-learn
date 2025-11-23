package org.example.boundedbuffer;

public interface BlockingBuffer<T> {

    void produce(T item) throws InterruptedException;

    T consume() throws InterruptedException;

    int size();
}
