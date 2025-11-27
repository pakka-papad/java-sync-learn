package org.example.resourcepool;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BlockingResourcePool<T> implements ResourcePool<T> {

    private final Queue<T> resources;
    private final Semaphore semaphore;
    private final Supplier<T> resourceFactory;
    private final Predicate<T> resourceValidator;

    BlockingResourcePool(int maxResources, Supplier<T> resourceFactory, Predicate<T> resourceValidator) {
        this.resourceFactory = resourceFactory;
        this.resourceValidator = resourceValidator;
        resources = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < maxResources; i++) {
            resources.add(resourceFactory.get());
        }
        semaphore = new Semaphore(maxResources);
    }

    @Override
    public T acquire() throws InterruptedException {
        semaphore.acquire();
        return resources.poll();
    }

    @Override
    public T acquire(long timeout, TimeUnit unit) throws InterruptedException {
        if (!semaphore.tryAcquire(timeout, unit)) {
            return null;
        }
        return resources.poll();
    }

    @Override
    public void release(T resource) throws InterruptedException {
        if (!resourceValidator.test(resource)) {
            resource = resourceFactory.get();
        }
        resources.offer(resource);
        semaphore.release();
    }

    @Override
    public int availableCount() {
        return semaphore.availablePermits();
    }
}
