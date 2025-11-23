package org.example.singleton;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ResourceConsumerTest {

    private static final int NUM_THREADS = 100;

    // --- EagerInit Tests ---
    @Test
    void testEagerInit_returnsSameInstance() {
        ResourceConsumer.EagerInit instance1 = ResourceConsumer.EagerInit.getInstance();
        ResourceConsumer.EagerInit instance2 = ResourceConsumer.EagerInit.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testEagerInit_isThreadSafe() throws InterruptedException {
        testThreadSafety(ResourceConsumer.EagerInit::getInstance);
    }

    // --- SyncInit Tests ---
    @Test
    void testSyncInit_returnsSameInstance() {
        ResourceConsumer.SyncInit instance1 = ResourceConsumer.SyncInit.getInstance();
        ResourceConsumer.SyncInit instance2 = ResourceConsumer.SyncInit.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testSyncInit_isThreadSafe() throws InterruptedException {
        testThreadSafety(ResourceConsumer.SyncInit::getInstance);
    }

    // --- DoubleCheckedLocking Tests ---
    @Test
    void testDoubleCheckedLocking_returnsSameInstance() {
        ResourceConsumer.DoubleCheckedLocking instance1 = ResourceConsumer.DoubleCheckedLocking.getInstance();
        ResourceConsumer.DoubleCheckedLocking instance2 = ResourceConsumer.DoubleCheckedLocking.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testDoubleCheckedLocking_isThreadSafe() throws InterruptedException {
        testThreadSafety(ResourceConsumer.DoubleCheckedLocking::getInstance);
    }

    // --- InnerStaticClass Tests ---
    @Test
    void testInnerStaticClass_returnsSameInstance() {
        ResourceConsumer.InnerStaticClass instance1 = ResourceConsumer.InnerStaticClass.getInstance();
        ResourceConsumer.InnerStaticClass instance2 = ResourceConsumer.InnerStaticClass.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testInnerStaticClass_isThreadSafe() throws InterruptedException {
        testThreadSafety(ResourceConsumer.InnerStaticClass::getInstance);
    }

    // --- Enum Singleton Tests ---
    @Test
    void testEnumSingleton_returnsSameInstance() {
        ResourceConsumer.ResourceSingleEnum instance1 = ResourceConsumer.ResourceSingleEnum.getInstance();
        ResourceConsumer.ResourceSingleEnum instance2 = ResourceConsumer.ResourceSingleEnum.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testEnumSingleton_isThreadSafe() throws InterruptedException {
        testThreadSafety(ResourceConsumer.ResourceSingleEnum::getInstance);
    }


    /**
     * Helper method to test the thread-safety of a singleton implementation.
     *
     * @param singletonSupplier A supplier function that calls the getInstance() method of the singleton.
     * @throws InterruptedException if the test is interrupted.
     */
    private <T> void testThreadSafety(java.util.function.Supplier<T> singletonSupplier) throws InterruptedException {
        Set<T> instances = Collections.newSetFromMap(new ConcurrentHashMap<>());
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                try {
                    instances.add(singletonSupplier.get());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(1, instances.size(), "All threads should have received the same singleton instance.");
    }
}
