package org.example.boundedbuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class LinkedBoundedBufferTest {

    private LinkedBoundedBuffer<Integer> buffer;

    @BeforeEach
    void setUp() {
        buffer = new LinkedBoundedBuffer<>(10);
    }

    @Test
    void testProduceAndConsume() throws InterruptedException {
        buffer.produce(42);
        assertEquals(1, buffer.size());
        assertEquals(42, buffer.consume());
        assertEquals(0, buffer.size());
    }

    @Test
    void testBufferIsEmptyOnConstruction() {
        assertEquals(0, buffer.size());
    }

    @Test
    @Timeout(1)
    void testConsumerBlocksWhenBufferIsEmpty() throws InterruptedException {
        final AtomicBoolean consumed = new AtomicBoolean(false);
        Thread consumer = new Thread(() -> {
            try {
                buffer.consume();
                consumed.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumer.start();
        // Give consumer time to start and block
        Thread.sleep(100);
        assertFalse(consumed.get());
        // With ReentrantLock, the state is RUNNABLE while waiting on a condition
        // but we can check that it's alive and the action hasn't completed.

        buffer.produce(1);
        // Wait for consumer to finish
        consumer.join();
        assertTrue(consumed.get());
    }

    @Test
    @Timeout(1)
    void testProducerBlocksWhenBufferIsFull() throws InterruptedException {
        // Fill the buffer
        for (int i = 0; i < 10; i++) {
            buffer.produce(i);
        }
        assertEquals(10, buffer.size());

        final AtomicBoolean produced = new AtomicBoolean(false);
        Thread producer = new Thread(() -> {
            try {
                buffer.produce(99);
                produced.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        // Give producer time to start and block
        Thread.sleep(100);
        assertFalse(produced.get());
        // With ReentrantLock, the state is RUNNABLE while waiting on a condition

        buffer.consume();
        // Wait for producer to finish
        producer.join();
        assertTrue(produced.get());
    }

    @Test
    void testOrderIsMaintained() throws InterruptedException {
        LinkedBoundedBuffer<Integer> smallBuffer = new LinkedBoundedBuffer<>(3);
        // Fill buffer
        smallBuffer.produce(1);
        smallBuffer.produce(2);
        smallBuffer.produce(3);

        // Consume two, making space
        assertEquals(1, smallBuffer.consume());
        assertEquals(2, smallBuffer.consume());

        // Produce two more
        smallBuffer.produce(4);
        smallBuffer.produce(5);

        // Consume remaining items to check FIFO order
        assertEquals(3, smallBuffer.consume());
        assertEquals(4, smallBuffer.consume());
        assertEquals(5, smallBuffer.consume());
    }

    @Test
    @Timeout(5)
    void testConcurrentProducersAndConsumers() throws InterruptedException {
        final int numThreads = 4;
        final int itemsPerThread = 100;
        final int totalItems = numThreads * itemsPerThread;
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads * 2);
        final CountDownLatch latch = new CountDownLatch(numThreads * 2);
        final List<Integer> consumedItems = Collections.synchronizedList(new ArrayList<>());
        final AtomicInteger producedCounter = new AtomicInteger();

        // Producers
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < itemsPerThread; j++) {
                        buffer.produce(producedCounter.getAndIncrement());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Consumers
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < itemsPerThread; j++) {
                        consumedItems.add(buffer.consume());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test timed out, possible deadlock.");
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS), "Executor did not terminate in time.");

        assertEquals(totalItems, consumedItems.size());
        List<Integer> expectedItems = IntStream.range(0, totalItems).boxed().collect(Collectors.toList());
        Collections.sort(consumedItems);
        assertEquals(expectedItems, consumedItems);
    }

    @Test
    @Timeout(1)
    void testInterruptProducerWhileWaiting() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            buffer.produce(i);
        }

        Thread producer = new Thread(() -> {
            try {
                buffer.produce(99);
                fail("Expected InterruptedException was not thrown");
            } catch (InterruptedException e) {
                // This is expected
            }
        });

        producer.start();
        Thread.sleep(100); // Allow thread to block
        producer.interrupt();
        producer.join();
        assertTrue(producer.isInterrupted() || !producer.isAlive());
    }

    @Test
    @Timeout(1)
    void testInterruptConsumerWhileWaiting() throws InterruptedException {
        Thread consumer = new Thread(() -> {
            try {
                buffer.consume();
                fail("Expected InterruptedException was not thrown");
            } catch (InterruptedException e) {
                // This is expected
            }
        });

        consumer.start();
        Thread.sleep(100); // Allow thread to block
        consumer.interrupt();
        consumer.join();
        assertTrue(consumer.isInterrupted() || !consumer.isAlive());
    }

    @Test
    @Timeout(1)
    void testCanProduceAfterConsumerIsInterrupted() throws InterruptedException {
        Thread consumer = new Thread(() -> {
            try {
                buffer.consume();
                fail("Expected InterruptedException was not thrown");
            } catch (InterruptedException e) {
                // This is expected
            }
        });

        consumer.start();
        Thread.sleep(100); // Allow thread to block
        consumer.interrupt();

        buffer.produce(1);

        assertEquals(1, buffer.consume());
    }
}
