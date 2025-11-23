package org.example.boundedbuffer;

public interface BlockingBuffer<T> {

    public void produce(T item) throws InterruptedException;

    public T consume() throws InterruptedException;
}
