package org.example.cyclicbarrier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AltCyclicBarrierTest {

    @Test
    @Timeout(5)
    void testConstructorWithInvalidParties() throws InterruptedException {
        var parties = List.of(0, -1, -3, -9090);
        for (var count : parties) {
            assertThrows(IllegalArgumentException.class, () -> new AltCyclicBarrier(count));
        }
    }

    @Test
    @Timeout(5)
    void testGetParties() throws InterruptedException {
        // Given
        final int parties = 6;
        var barrier = new AltCyclicBarrier(parties);

        // Then
        assertEquals(parties, barrier.getParties());
    }

    @Test
    @Timeout(5)
    void testSuccessfulBarrierCompletion() throws InterruptedException {
        // Given
        final int parties = 6;
        final var barrier = new AltCyclicBarrier(parties);

        // When
        final var idxs = new CopyOnWriteArraySet<Integer>();
        final var threads = new ArrayList<Thread>(parties);
        Runnable action = () -> {
            try {
                var idx = barrier.await();
                idxs.add(idx);
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        };
        for (int i = 0; i < parties; i++) {
            var th = new Thread(action);
            threads.add(th);
            th.start();
        }

        // Then
        for (var th : threads) {
            th.join();
        }
        assertEquals(parties, idxs.size());
        for (int i = 0; i < parties; i++) {
            assertTrue(idxs.contains(i));
        }
    }

    @Test
    @Timeout(5)
    void testBarrierIsCyclic() throws InterruptedException {
        // Given
        final int parties = 6;
        final var barrier = new AltCyclicBarrier(parties);

        // When
        final var idxs = new CopyOnWriteArraySet<Integer>();
        final var threads = new ArrayList<Thread>(parties);
        Runnable action = () -> {
            try {
                var idx = barrier.await();
                idxs.add(idx);
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        };
        for (int rep = 0; rep < 3; rep++) {
            idxs.clear();
            threads.clear();
            for (int i = 0; i < parties; i++) {
                var th = new Thread(action);
                threads.add(th);
                th.start();
            }
            for (var th : threads) {
                th.join();
            }
        }

        // Then
        assertEquals(parties, idxs.size());
        for (int i = 0; i < parties; i++) {
            assertTrue(idxs.contains(i));
        }
    }

    @Test
    @Timeout(5)
    void testGetNumberWaiting() throws InterruptedException {
        // Given
        final int parties = 6;
        final var barrier = new AltCyclicBarrier(parties);

        // When
        Runnable action = () -> {
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        };
        var th = new Thread(action);
        th.start();

        // Then
        Thread.sleep(200);
        assertEquals(1, barrier.getNumberWaiting());
    }

    @Test
    @Timeout(5)
    void testThreadInterruptedBeforeAwait() throws InterruptedException, BrokenBarrierException, TimeoutException {
        // Given
        final int parties = 6;
        final var barrier = new AltCyclicBarrier(parties);

        // When
        var expThrown = new AtomicReference<Exception>(null);
        Runnable action = () -> {
            Thread.currentThread().interrupt();
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                expThrown.set(e);
            }
        };
        var th = new Thread(action);
        th.start();

        // Then
        Thread.sleep(200);
        assertNotNull(expThrown.get());
        assertInstanceOf(InterruptedException.class, expThrown.get());
    }

    @Test
    @Timeout(5)
    void testThreadInterruptedWhileWaiting() throws InterruptedException {
        // Given
        final int parties = 6;
        final var barrier = new AltCyclicBarrier(parties);

        // When
        var expThrown = new AtomicReference<Exception>(null);
        Runnable action = () -> {
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                expThrown.set(e);
            }
        };
        var th = new Thread(action);
        th.start();

        // Then
        Thread.sleep(200);
        assertEquals(1, barrier.getNumberWaiting());
        th.interrupt();
        th.join();
        assertTrue(barrier.isBroken());
        assertNotNull(expThrown.get());
        assertInstanceOf(InterruptedException.class, expThrown.get());
    }

    @Test
    @Timeout(5)
    void testBarrierBrokenByInterruption() throws InterruptedException {
        // Given
        final int parties = 6;
        final var barrier = new AltCyclicBarrier(parties);

        // When
        var expThrown1 = new AtomicReference<Exception>(null);
        Runnable action1 = () -> {
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                expThrown1.set(e);
            }
        };
        var thread1 = new Thread(action1);
        thread1.start();
        var expThrown2 = new AtomicReference<Exception>(null);
        Runnable action2 = () -> {
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                expThrown2.set(e);
            }
        };
        var thread2 = new Thread(action2);
        thread2.start();

        // Then
        Thread.sleep(200);
        assertEquals(2, barrier.getNumberWaiting());
        thread2.interrupt();
        thread2.join();
        thread1.join();
        assertTrue(barrier.isBroken());
        assertNotNull(expThrown1.get());
        assertInstanceOf(BrokenBarrierException.class, expThrown1.get());
        assertNotNull(expThrown2.get());
        assertInstanceOf(InterruptedException.class, expThrown2.get());
    }

    @Test
    @Timeout(5)
    void testAwaitOnBrokenBarrier() throws InterruptedException, BrokenBarrierException {
        // Given
        final int parties = 6;
        final var barrier = new AltCyclicBarrier(parties);
        Runnable action = () -> {
            try {
                Thread.currentThread().interrupt();
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
            }
        };
        var thread = new Thread(action);
        thread.start();

        // When
        thread.join();
        assertTrue(barrier.isBroken());
        Exception expThrown = null;
        try {
            barrier.await();
        } catch (Exception e) {
            expThrown = e;
        }

        // Then
        assertTrue(barrier.isBroken());
        assertNotNull(expThrown);
        assertInstanceOf(BrokenBarrierException.class, expThrown);
    }

    @Test
    @Timeout(5)
    void testIsBrokenStatus() throws InterruptedException {
        // Given
        final int parties = 6;
        final var barrier = new AltCyclicBarrier(parties);

        // When
        Thread.currentThread().interrupt();
        try {
            barrier.await();
        } catch (Exception e) {

        }

        // Then
        assertTrue(barrier.isBroken());
    }

    @Test
    @Timeout(5)
    void testAwaitWithTimeoutSucceeds() throws InterruptedException, BrokenBarrierException, TimeoutException {
        // Given
        final int parties = 2;
        final var barrier = new AltCyclicBarrier(parties);

        // When
        final var expThrown = new AtomicReference<Exception>(null);
        Runnable action = () -> {
            try {
                barrier.await(600, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                expThrown.set(e);
            }
        };
        var thread = new Thread(action);
        thread.start();
        barrier.await();

        // Then
        assertFalse(barrier.isBroken());
        assertNull(expThrown.get());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    @Timeout(5)
    void testAwaitWithTimeoutFails() throws InterruptedException {
        // Given
        final int parties = 2;
        final var barrier = new AltCyclicBarrier(parties);

        // When
        final var expThrown = new AtomicReference<Exception>(null);
        Runnable action = () -> {
            try {
                barrier.await(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                expThrown.set(e);
            }
        };
        var thread = new Thread(action);
        thread.start();

        // Then
        thread.join();
        assertTrue(barrier.isBroken());
        assertNotNull(expThrown.get());
        assertInstanceOf(TimeoutException.class, expThrown.get());
    }

    @Test
    @Timeout(5)
    void testBarrierBrokenByTimeout() throws InterruptedException {
        // Given
        final int parties = 6;
        final var barrier = new AltCyclicBarrier(parties);

        // When
        var expThrown1 = new AtomicReference<Exception>(null);
        Runnable action1 = () -> {
            try {
                barrier.await(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                expThrown1.set(e);
            }
        };
        var thread1 = new Thread(action1);
        thread1.start();
        var expThrown2 = new AtomicReference<Exception>(null);
        Runnable action2 = () -> {
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                expThrown2.set(e);
            }
        };
        var thread2 = new Thread(action2);
        thread2.start();

        // Then
        thread2.join();
        thread1.join();
        assertTrue(barrier.isBroken());
        assertNotNull(expThrown1.get());
        assertInstanceOf(TimeoutException.class, expThrown1.get());
        assertNotNull(expThrown2.get());
        assertInstanceOf(BrokenBarrierException.class, expThrown2.get());
    }

    @Test
    @Timeout(5)
    void testBarrierActionIsExecuted() throws InterruptedException, BrokenBarrierException {
        // Given
        final var actionDone = new AtomicBoolean(false);
        final int parties = 1;
        final var barrier = new AltCyclicBarrier(parties, () -> {
            actionDone.set(true);
        });

        // When
        barrier.await();

        // Then
        assertTrue(actionDone.get());
    }

    @Test
    @Timeout(5)
    void testBarrierBrokenByActionException() throws InterruptedException, BrokenBarrierException {
        // Given
        final int parties = 2;
        final var barrier = new AltCyclicBarrier(parties, () -> {
            throw new RuntimeException();
        });

        // When
        final var expThrown = new AtomicReference<Exception>(null);
        Runnable action = () -> {
            try {
                barrier.await(200, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                expThrown.set(e);
            }
        };
        var thread = new Thread(action);
        thread.start();
        while (barrier.getNumberWaiting() != 1) {
            Thread.sleep(100);
            // to ensure the current thread is the second thread
        }

        try {
            barrier.await();
        } catch (Exception e) {

        }

        // Then
        thread.join();
        assertTrue(barrier.isBroken());
        assertNotNull(expThrown.get());
        assertInstanceOf(BrokenBarrierException.class, expThrown.get());
    }

    @Test
    @Timeout(5)
    void testResettingTheBarrier() throws InterruptedException {
        // Given
        final int parties = 6;
        final var barrier = new AltCyclicBarrier(parties);

        // When
        final var expThrown = new AtomicReference<Exception>(null);
        Runnable action = () -> {
            try {
                barrier.await();
            } catch (Exception e) {
                expThrown.set(e);
            }
        };
        var thread = new Thread(action);
        thread.start();

        // Then
        while (barrier.getNumberWaiting() != 1) {
            Thread.sleep(100);
        }
        barrier.reset();
        thread.join();
        assertNotNull(expThrown.get());
        assertInstanceOf(BrokenBarrierException.class, expThrown.get());
    }

    @Test
    @Timeout(5)
    void testBarrierIsReusableAfterReset() throws InterruptedException {
        // Given
        final int parties = 6;
        final var barrier = new AltCyclicBarrier(parties);
        Runnable initAction = () -> {
            try {
                barrier.await();
            } catch (Exception e) {
            }
        };
        var thread = new Thread(initAction);
        thread.start();
        while (barrier.getNumberWaiting() != 1) {
            Thread.sleep(100);
        }
        barrier.reset();

        // When
        final var idxs = new CopyOnWriteArraySet<Integer>();
        final var threads = new ArrayList<Thread>(parties);
        Runnable action = () -> {
            try {
                var idx = barrier.await();
                idxs.add(idx);
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        };
        for (int i = 0; i < parties; i++) {
            var th = new Thread(action);
            threads.add(th);
            th.start();
        }

        // Then
        for (var th : threads) {
            th.join();
        }
        assertEquals(parties, idxs.size());
        for (int i = 0; i < parties; i++) {
            assertTrue(idxs.contains(i));
        }
    }
}
