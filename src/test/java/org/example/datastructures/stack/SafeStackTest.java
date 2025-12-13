package org.example.datastructures.stack;

import org.example.datastructures.SampleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SafeStackTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testPushAndPop() throws InterruptedException {
        // Given
        final var stack = new SafeStack<SampleType>();
        final var item = new SampleType("test-item");

        // When
        stack.push(item);
        var retrieved = stack.pop();

        // Then
        assertEquals(item, retrieved);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testPeek() throws InterruptedException {
        // Given
        final var stack = new SafeStack<SampleType>();
        final var item = new SampleType("test-item");

        // When
        stack.push(item);

        // Then
        assertEquals(item, stack.peek());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testSize() throws InterruptedException {
        // Given
        final var stack = new SafeStack<SampleType>();

        // When
        final int count = 6;
        for (int i = 0; i < count; i++) {
            stack.push(new SampleType("item-" + i));
        }

        // Then
        assertEquals(count, stack.size());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testIsEmpty() throws InterruptedException {
        // Given
        final var stack = new SafeStack<SampleType>();

        // When & Then
        assertTrue(stack.isEmpty());
        final int count = 6;
        for (int i = 0; i < count; i++) {
            stack.push(new SampleType("item-" + i));
        }
        assertFalse(stack.isEmpty());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testPopFromEmptyStackThrowsException() throws InterruptedException {
        // Given
        final var stack = new SafeStack<SampleType>();

        // When & Then
        assertThrows(NoSuchElementException.class, stack::pop);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testPeekFromEmptyStackThrowsException() throws InterruptedException {
        // Given
        final var stack = new SafeStack<SampleType>();

        // When & Then
        assertThrows(NoSuchElementException.class, stack::peek);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testConcurrentPushAndPop() throws InterruptedException {
        // Given
        final var stack = new SafeStack<SampleType>();
        final int count = 6;
        final var items = new ArrayList<SampleType>(count);
        for (int i = 0; i < count; i++) {
            items.add(new SampleType("item-" + i));
        }

        // When
        final var idx = new AtomicInteger(0);
        final var latch = new CountDownLatch(count);
        final var set = Collections.synchronizedSet(new HashSet<>(count));
        Runnable action = () -> {
            latch.countDown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            var myIdx = idx.getAndIncrement();
            stack.push(items.get(myIdx));
            set.add(stack.pop());
        };
        final var threads = new ArrayList<Thread>();
        for (int i = 0; i < count; i++) {
            final var thread = new Thread(action);
            threads.add(thread);
            thread.start();
        }

        // Then
        for (int i = 0; i < count; i++) {
            threads.get(i).join();
        }
        assertEquals(count, set.size());
        assertTrue(stack.isEmpty());
        for (var item : items) {
            assertTrue(set.contains(item));
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testConcurrentPush() throws InterruptedException {
        // Given
        final var stack = new SafeStack<SampleType>();
        final int count = 6;
        final var items = new ArrayList<SampleType>(count);
        for (int i = 0; i < count; i++) {
            items.add(new SampleType("item-" + i));
        }

        // When
        final var idx = new AtomicInteger(0);
        final var latch = new CountDownLatch(count);
        final var set = Collections.synchronizedSet(new HashSet<>(count));
        Runnable action = () -> {
            latch.countDown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            var myIdx = idx.getAndIncrement();
            var toPush = items.get(myIdx);
            set.add(toPush);
            stack.push(toPush);
        };
        final var threads = new ArrayList<Thread>();
        for (int i = 0; i < count; i++) {
            final var thread = new Thread(action);
            threads.add(thread);
            thread.start();
        }

        // Then
        for (int i = 0; i < count; i++) {
            threads.get(i).join();
        }
        assertEquals(count, set.size());
        assertFalse(stack.isEmpty());
        assertEquals(count, stack.size());
        for (var item : items) {
            assertTrue(set.contains(item));
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testConcurrentPop() throws InterruptedException {
        // Given
        final var stack = new SafeStack<SampleType>();
        final int count = 6;
        final var items = new ArrayList<SampleType>(count);
        for (int i = 0; i < count; i++) {
            final var item = new SampleType("item-" + i);
            items.add(item);
            stack.push(item);
        }

        // When
        final var latch = new CountDownLatch(count);
        final var set = Collections.synchronizedSet(new HashSet<>(count));
        Runnable action = () -> {
            latch.countDown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            set.add(stack.pop());
        };
        final var threads = new ArrayList<Thread>();
        for (int i = 0; i < count; i++) {
            final var thread = new Thread(action);
            threads.add(thread);
            thread.start();
        }

        // Then
        for (int i = 0; i < count; i++) {
            threads.get(i).join();
        }
        assertEquals(count, set.size());
        assertTrue(stack.isEmpty());
        for (var item : items) {
            assertTrue(set.contains(item));
        }
    }
}
