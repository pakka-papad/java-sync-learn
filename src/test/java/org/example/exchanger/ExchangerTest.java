package org.example.exchanger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ExchangerTest {

    @Test
    @Timeout(2)
    void testBasicExchange() throws InterruptedException {
        Exchanger<String> exchanger = new Exchanger<>();
        AtomicReference<String> result1 = new AtomicReference<>();
        AtomicReference<String> result2 = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            try {
                result1.set(exchanger.exchange("Data1"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                result2.set(exchanger.exchange("Data2"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        t1.start();
        t2.start();
        assertTrue(latch.await(1, TimeUnit.SECONDS));

        assertEquals("Data2", result1.get());
        assertEquals("Data1", result2.get());
    }

    @Test
    @Timeout(2)
    void testMultipleExchanges() throws InterruptedException {
        Exchanger<Integer> exchanger = new Exchanger<>();
        
        for (int i = 0; i < 3; i++) {
            final int val = i;
            AtomicReference<Integer> r1 = new AtomicReference<>();
            AtomicReference<Integer> r2 = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(2);

            new Thread(() -> {
                try {
                    r1.set(exchanger.exchange(val));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();

            new Thread(() -> {
                try {
                    r2.set(exchanger.exchange(val + 10));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();

            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertEquals(val + 10, r1.get());
            assertEquals(val, r2.get());
        }
    }

    @Test
    @Timeout(2)
    void testTryExchangeTimeout() throws InterruptedException {
        Exchanger<String> exchanger = new Exchanger<>();
        
        long start = System.currentTimeMillis();
        assertThrows(TimeoutException.class, () -> 
            exchanger.tryExchange(100, TimeUnit.MILLISECONDS, "Data")
        );
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 100);

        // Verify we can still exchange after a timeout
        CountDownLatch latch = new CountDownLatch(2);
        Thread t1 = new Thread(() -> {
            try {
                exchanger.exchange("NewData1");
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                exchanger.exchange("NewData2");
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t1.start();
        t2.start();
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(2)
    void testInterruptionCleanup() throws InterruptedException {
        Exchanger<String> exchanger = new Exchanger<>();
        CountDownLatch threadStarted = new CountDownLatch(1);
        CountDownLatch threadInterrupted = new CountDownLatch(1);

        Thread t1 = new Thread(() -> {
            try {
                threadStarted.countDown();
                exchanger.exchange("InterruptedData");
            } catch (InterruptedException e) {
                threadInterrupted.countDown();
            }
        });

        t1.start();
        assertTrue(threadStarted.await(1, TimeUnit.SECONDS));
        Thread.sleep(100); // Wait for t1 to block in exchange
        t1.interrupt();
        assertTrue(threadInterrupted.await(1, TimeUnit.SECONDS));

        // Verify we can still exchange after an interruption (proves cleanup worked)
        AtomicReference<String> r1 = new AtomicReference<>();
        AtomicReference<String> r2 = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);

        new Thread(() -> {
            try {
                r1.set(exchanger.exchange("DataA"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        }).start();

        new Thread(() -> {
            try {
                r2.set(exchanger.exchange("DataB"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        }).start();

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals("DataB", r1.get());
        assertEquals("DataA", r2.get());
    }

    @Test
    @Timeout(5)
    void testConcurrentExchanges() throws InterruptedException {
        Exchanger<Integer> exchanger = new Exchanger<>();
        int numPairs = 10;
        CountDownLatch latch = new CountDownLatch(numPairs * 2);
        
        for (int i = 0; i < numPairs; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    Integer result = exchanger.exchange(id);
                    assertNotEquals(id, result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
            
            new Thread(() -> {
                try {
                    Integer result = exchanger.exchange(id + 100);
                    assertNotEquals(id + 100, result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));
    }
}
