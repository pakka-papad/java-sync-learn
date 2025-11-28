package org.example.countdownlatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AltCDLatchTest {

    @Test
    void testConstructorValidCount() {
        var latch = new AltCDLatch(2);
        assertEquals(2, latch.getCount());
    }

    @Test
    void testConstructorInvalidCountThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            var latch = new AltCDLatch(-2);
        });
    }

    @Test
    @Timeout(5)
    void testAwaitWhenCountIsZero() throws InterruptedException {
        // Given
        int count = 0;
        var latch = new AltCDLatch(count);

        // When
        var t1 = System.nanoTime();
        latch.await();
        var t2 = System.nanoTime();

        // Then
        assertEquals(0, latch.getCount());
        assertTrue(t2 - t1 <= TimeUnit.MILLISECONDS.toNanos(100));
    }

    @Test
    @Timeout(5)
    void testAwaitWhenCountDecrements() throws InterruptedException {
        // Given
        int count = 4;
        var latch = new AltCDLatch(count);

        // When
        final var unblocked = new AtomicBoolean(false);
        Runnable waiterAction = () -> {
            try {
                latch.await();
                unblocked.set(true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        var waiter = new Thread(waiterAction);
        waiter.start();
        Thread.sleep(100);
        for (int i = 0; i < count; i++) {
            latch.countDown();
        }

        // Then
        waiter.join();
        assertEquals(0, latch.getCount());
        assertTrue(unblocked.get());
    }

    @Test
    @Timeout(5)
    void testAwaitTimeoutWhenCountIsZero() throws InterruptedException {
        // Given
        int count = 0;
        var latch = new AltCDLatch(count);

        // When
        var t1 = System.nanoTime();
        latch.await(200, TimeUnit.MILLISECONDS);
        var t2 = System.nanoTime();

        // Then
        assertEquals(0, latch.getCount());
        assertTrue(t2 - t1 <= TimeUnit.MILLISECONDS.toNanos(100));
    }

    @Test
    @Timeout(5)
    void testAwaitTimeoutWhenCountDecrementsWithinTimeout() throws InterruptedException {
        // Given
        int count = 4;
        var latch = new AltCDLatch(count);

        // When
        final var timeTake = new AtomicLong(Long.MAX_VALUE);
        Runnable waiterAction = () -> {
            try {
                var t1 = System.nanoTime();
                latch.await(400, TimeUnit.MILLISECONDS);
                var t2 = System.nanoTime();
                timeTake.set(t2 - t1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        var waiter = new Thread(waiterAction);
        waiter.start();
        Thread.sleep(100);
        for (int i = 0; i < count; i++) {
            latch.countDown();
        }

        // Then
        waiter.join();
        assertEquals(0, latch.getCount());
        assertTrue(timeTake.get() < TimeUnit.MILLISECONDS.toNanos(400L));
    }

    @Test
    @Timeout(5)
    void testAwaitTimeoutWhenTimeoutOccurs() throws InterruptedException {
        // Given
        int count = 4;
        var latch = new AltCDLatch(count);

        // When
        final var timeTake = new AtomicLong(Long.MAX_VALUE);
        Runnable waiterAction = () -> {
            try {
                var t1 = System.nanoTime();
                latch.await(400, TimeUnit.MILLISECONDS);
                var t2 = System.nanoTime();
                timeTake.set(t2 - t1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        var waiter = new Thread(waiterAction);
        waiter.start();
        Thread.sleep(450);
        for (int i = 0; i < count; i++) {
            latch.countDown();
        }

        // Then
        waiter.join();
        assertEquals(0, latch.getCount());
        assertTrue(timeTake.get() >= TimeUnit.MILLISECONDS.toNanos(400L));
    }

    @Test
    void testCountDownBehavior() {
        // Given
        int count = 4;
        var latch = new AltCDLatch(count);

        // When & Then
        assertEquals(count, latch.getCount());
        latch.countDown();
        latch.countDown();
        assertEquals(count - 2, latch.getCount());
        latch.countDown();
        assertEquals(count - 3, latch.getCount());
    }
}
