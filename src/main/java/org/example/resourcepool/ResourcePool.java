package org.example.resourcepool;

import java.util.concurrent.TimeUnit;

public interface ResourcePool<T> {

    T acquire() throws InterruptedException;

    T acquire(long timeout, TimeUnit unit) throws InterruptedException;

    void release(T resource) throws InterruptedException;

    int availableCount();
}
