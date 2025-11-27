package org.example.resourcepool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class BlockingResourcePoolTest {

    private static class Resource {
        final int id;

        Resource(int id) {
            this.id = id;
        }
    }

    private final Supplier<Resource> supplier = new Supplier<>() {
        private final AtomicInteger ids = new AtomicInteger(1);

        @Override
        public Resource get() {
            return new Resource(ids.getAndIncrement());
        }
    };

    private final Predicate<Resource> validator = (resource) -> (true);

    @Test
    @Timeout(5)
    void testInitializationFixedPoolSize() {
        // Given
        int count = 4;
        var pool = new BlockingResourcePool<>(count, supplier, validator);

        // Then
        assertEquals(count, pool.availableCount());
    }

    @Test
    @Timeout(5)
    void testAcquireAndReleaseConnection() throws InterruptedException {
        // Given
        int count = 4;
        var pool = new BlockingResourcePool<>(count, supplier, validator);

        // When & Then
        var res1 = pool.acquire();
        assertEquals(count - 1, pool.availableCount());
        pool.release(res1);
        assertEquals(count, pool.availableCount());
    }

    @Test
    @Timeout(5)
    void testAcquireBlocksWhenNoConnectionsAvailable() throws InterruptedException {
        // Given
        int count = 4;
        var pool = new BlockingResourcePool<>(count, supplier, validator);

        // When
        Runnable threadAction = () -> {
            try {
                var res = pool.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        for (int i = 0; i < count; i++) {
            var thread = new Thread(threadAction);
            thread.start();
        }
        Thread.sleep(100);

        // Then
        assertEquals(0, pool.availableCount());
        Runnable tryAcquireAction = () -> {
            try {
                pool.acquire();
                fail("Resource is acquired");
            } catch (InterruptedException ie) {

            }
        };
        var tryAcquireThread = new Thread(tryAcquireAction);
        tryAcquireThread.start();
        Thread.sleep(2000);
        tryAcquireThread.interrupt();
        assertEquals(0, pool.availableCount());
    }

    @Test
    @Timeout(5)
    void testAcquireWithTimeoutSuccess() throws InterruptedException {
        // Given
        int count = 4;
        var pool = new BlockingResourcePool<>(count, supplier, validator);

        // When
        var goAhead = new AtomicBoolean(false);
        Runnable threadAction = () -> {
            try {
                var res = pool.acquire();
                while (!goAhead.get()) {
                    Thread.sleep(100);
                }
                pool.release(res);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        for (int i = 0; i < count; i++) {
            var thread = new Thread(threadAction);
            thread.start();
        }
        Thread.sleep(300);

        // Then
        assertEquals(0, pool.availableCount());
        Runnable tryAcquireAction = () -> {
            try {
                var res = pool.acquire(800, TimeUnit.MILLISECONDS);
                assertNotNull(res);
            } catch (InterruptedException ie) {

            }
        };
        var tryAcquireThread = new Thread(tryAcquireAction);
        tryAcquireThread.start();
        goAhead.set(true);
        Thread.sleep(1000);
        assertEquals(count - 1, pool.availableCount());
    }

    @Test
    @Timeout(5)
    void testAcquireWithTimeoutFailure() throws InterruptedException {
        // Given
        int count = 4;
        var pool = new BlockingResourcePool<>(count, supplier, validator);

        // When
        var goAhead = new AtomicBoolean(false);
        Runnable threadAction = () -> {
            try {
                var res = pool.acquire();
                while (!goAhead.get()) {
                    Thread.sleep(100);
                }
                pool.release(res);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        for (int i = 0; i < count; i++) {
            var thread = new Thread(threadAction);
            thread.start();
        }
        Thread.sleep(300);

        // Then
        assertEquals(0, pool.availableCount());
        Runnable tryAcquireAction = () -> {
            try {
                var res = pool.acquire(200, TimeUnit.MILLISECONDS);
                assertNull(res);
            } catch (InterruptedException ie) {

            }
        };
        var tryAcquireThread = new Thread(tryAcquireAction);
        tryAcquireThread.start();
        Thread.sleep(500);
        goAhead.set(true);
        Thread.sleep(300);
        assertEquals(count, pool.availableCount());
    }

    @Test
    @Timeout(5)
    void testResourceValidationOnReleaseValidResource() throws InterruptedException {
        // Given
        int count = 4;
        var pool = new BlockingResourcePool<>(count, supplier, validator);
        var validRes = pool.acquire();
        var validResId = validRes.id;
        pool.release(validRes);

        // When
        var validResAcquires = new AtomicInteger(0);
        Runnable threadAction = () -> {
            try {
                var res = pool.acquire();
                if (validResId == res.id) {
                    validResAcquires.getAndIncrement();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        for (int i = 0; i < count; i++) {
            var thread = new Thread(threadAction);
            thread.start();
        }
        Thread.sleep(100);

        // Then
        assertEquals(1, validResAcquires.get());
    }

    @Test
    @Timeout(5)
    void testResourceValidationOnReleaseInvalidResource() throws InterruptedException {
        // Given
        int count = 4;
        var pool = new BlockingResourcePool<>(count, supplier, (res) -> false);
        var inValidRes = pool.acquire();
        var inValidResId = inValidRes.id;
        pool.release(inValidRes);

        // When
        var inValidResAcquires = new AtomicInteger(0);
        Runnable threadAction = () -> {
            try {
                var res = pool.acquire();
                if (inValidResId == res.id) {
                    inValidResAcquires.getAndIncrement();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        for (int i = 0; i < count; i++) {
            var thread = new Thread(threadAction);
            thread.start();
        }
        Thread.sleep(100);

        // Then
        assertEquals(0, inValidResAcquires.get());
    }

    @Test
    @Timeout(5)
    void testAvailableCount() throws InterruptedException {
        // Given
        int count = 4;
        var pool = new BlockingResourcePool<>(count, supplier, validator);

        // When & Then
        var res1 = pool.acquire();
        assertEquals(count - 1, pool.availableCount());
        pool.release(res1);
        assertEquals(count, pool.availableCount());

        var res2 = pool.acquire();
        var res3 = pool.acquire();
        assertEquals(count - 2, pool.availableCount());
        pool.release(res3);
        assertEquals(count - 1, pool.availableCount());
        pool.release(res2);
        assertEquals(count, pool.availableCount());
    }

    @Test
    @Timeout(5)
    void testMultipleAcquireAndRelease() throws InterruptedException {
        // Given
        int count = 4;
        var pool = new BlockingResourcePool<>(count, supplier, validator);

        // When
        Runnable threadAction = () -> {
            try {
                for (int i = 0; i < 10; i++) {
                    var res = pool.acquire();
                    Thread.sleep(150);
                    pool.release(res);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        for (int i = 0; i < 3; i++) {
            var thread = new Thread(threadAction);
            thread.start();
        }
        Thread.sleep(2000);

        // Then
        assertEquals(count, pool.availableCount());
    }
}
