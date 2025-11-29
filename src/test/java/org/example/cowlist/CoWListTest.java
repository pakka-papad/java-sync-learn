package org.example.cowlist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class CoWListTest {

    @Test
    void testAddAndGet() {
        // Given
        var list = new CoWList<Integer>();

        // When & Then
        list.add(4);
        assertEquals(4, list.get(0));
        list.add(5);
        list.add(6);
        assertEquals(4, list.get(0));
        assertEquals(5, list.get(1));
        assertEquals(6, list.get(2));
    }

    @Test
    void testGetOutOfBounds() {
        // Given
        var list = new CoWList<Integer>();

        // When & Then
        var idxs1 = List.of(0, -1, 3, 999, -33);
        idxs1.forEach(it -> {
            assertThrows(IndexOutOfBoundsException.class, () -> {
                list.get(it);
            });
        });
        list.add(2);
        list.add(3);
        var idxs2 = List.of(4, 5, 777, -9, -80);
        idxs2.forEach(it -> {
            assertThrows(IndexOutOfBoundsException.class, () -> {
                list.get(it);
            });
        });
    }

    @Test
    void testSize() {
        // Given
        var list = new CoWList<Integer>();

        // When & Then
        assertEquals(0, list.size());
        list.add(4);
        assertEquals(1, list.size());
        list.add(5);
        list.add(6);
        assertEquals(3, list.size());
        list.add(7);
        assertEquals(4, list.size());
    }

    @Test
    void testRemoveExistingItem() {
        // Given
        var list = new CoWList<Integer>();

        // When & Then
        list.add(2);
        list.add(3);
        list.add(4);
        assertTrue(list.remove(2));
        assertEquals(3, list.get(0));
        assertEquals(4, list.get(1));
    }

    @Test
    void testRemoveNonExistingItem() {
        // Given
        var list = new CoWList<Integer>();

        // When & Then
        list.add(2);
        list.add(3);
        list.add(4);
        assertFalse(list.remove(5));
    }

    @Test
    void testRemoveNullItem() {
        // Given
        var list = new CoWList<Integer>();

        // When & Then
        list.add(2);
        list.add(3);
        list.add(4);
        assertFalse(list.remove(null));
        list.add(null);
        assertTrue(list.remove(null));
    }

    @Test
    void testRemoveAtValidIndex() {
        // Given
        var list = new CoWList<Integer>();

        // When & Then
        list.add(2);
        list.add(3);
        list.add(4);
        assertTrue(list.removeAt(1));
        assertEquals(2, list.get(0));
        assertEquals(4, list.get(1));
        assertTrue(list.removeAt(1));
        assertEquals(1, list.size());
        assertEquals(2, list.get(0));
    }

    @Test
    void testRemoveAtInvalidIndex() {
        // Given
        var list = new CoWList<Integer>();

        // When & Then
        list.add(2);
        list.add(3);
        list.add(4);
        assertThrows(IllegalArgumentException.class, () -> {
            list.removeAt(3);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            list.removeAt(-1);
        });
    }

    @Test
    void testIteratorBasic() {
        // Given
        var list = new CoWList<Integer>();
        list.add(2);
        list.add(3);
        list.add(4);

        // When
        var it = list.iterator();

        // Then
        assertTrue(it.hasNext());
        assertEquals(2, it.next());
        assertTrue(it.hasNext());
        assertEquals(3, it.next());
        it.forEachRemaining(num -> {
            assertEquals(4, num);
        });
    }

    @Test
    void testIteratorSnapshotBehavior() {
        // Given
        var list = new CoWList<Integer>();
        list.add(2);
        list.add(3);

        // When
        var it = list.iterator();
        list.add(5);

        // Then
        assertTrue(it.hasNext());
        assertEquals(2, it.next());
        assertTrue(it.hasNext());
        assertEquals(3, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testIteratorRemoveThrowsUnsupportedOperationException() {
        // Given
        var list = new CoWList<Integer>();
        list.add(2);
        list.add(3);

        // When
        var it = list.iterator();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void testIteratorForEachRemaining() {
        // Given
        var list = new CoWList<Integer>();
        list.add(2);
        list.add(3);

        // When
        var it = list.iterator();
        var items = new ArrayList<Integer>();
        it.forEachRemaining(items::add);

        // Then
        assertEquals(2, items.size());
        assertEquals(2, items.get(0));
        assertEquals(3, items.get(1));
    }

    @Test
    @Timeout(5)
    void testConcurrentAdd() throws InterruptedException {
        // Given
        var list = new CoWList<Integer>();

        // When
        int count = 4;
        var nums = new AtomicInteger(0);
        var barrier = new CountDownLatch(1);
        Runnable threadAction = () -> {
            try {
                barrier.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            list.add(nums.getAndIncrement());
        };
        var threads = new ArrayList<Thread>(count);
        for (int i = 0; i < count; i++) {
            var thread = new Thread(threadAction);
            thread.start();
            threads.add(thread);
        }
        barrier.countDown();
        for (int i = 0; i < count; i++) {
            threads.get(i).join();
        }

        // Then
        assertEquals(count, nums.get());
        assertEquals(count, list.size());
        var elements = new TreeSet<Integer>();
        for (int i = 0; i < count; i++) {
            elements.add(list.get(i));
        }
        for (int i = 0; i < count; i++) {
            assertTrue(elements.contains(i));
        }
    }

    @Test
    void testConcurrentRemove() throws InterruptedException {
        // Given
        var list = new CoWList<Integer>();
        list.add(0);
        list.add(1);
        list.add(2);
        list.add(3);

        // When
        int count = 4;
        var nums = new AtomicInteger(0);
        var barrier = new CountDownLatch(1);
        Runnable threadAction = () -> {
            try {
                barrier.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            list.remove(nums.getAndIncrement());
        };
        var threads = new ArrayList<Thread>(count);
        for (int i = 0; i < count; i++) {
            var thread = new Thread(threadAction);
            thread.start();
            threads.add(thread);
        }
        barrier.countDown();
        for (int i = 0; i < count; i++) {
            threads.get(i).join();
        }

        // Then
        assertEquals(count, nums.get());
        assertEquals(0, list.size());
    }

    @Test
    void testConcurrentAddAndRead() throws InterruptedException {
        // Given
        var list = new CoWList<Integer>();
        list.add(0);
        list.add(1);
        list.add(2);
        list.add(3);

        // When & Then
        int count = 4;
        var nums = new AtomicInteger(0);
        var barrier = new CountDownLatch(1);
        Runnable threadAction = () -> {
            try {
                barrier.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            var index = nums.getAndIncrement();
            for (int i = 0; i < 30000; i++) {
                assertEquals(index, list.get(index));
            }
        };
        var threads = new ArrayList<Thread>(count);
        for (int i = 0; i < count; i++) {
            var thread = new Thread(threadAction);
            thread.start();
            threads.add(thread);
        }
        barrier.countDown();
        for (int i = 10; i < 1000; i++) {
            list.add(i);
        }
        for (int i = 0; i < count; i++) {
            threads.get(i).join();
        }
    }

    @Test
    @Timeout(5)
    void testConcurrentIterator() throws InterruptedException {
        // Given
        var list = new CoWList<Integer>();
        int initialCount = 5;
        for (int i = 0; i < initialCount; i++) {
            list.add(i);
        }
        List<Integer> expectedIteratedElements = new ArrayList<>();
        for (int i = 0; i < initialCount; i++) {
            expectedIteratedElements.add(i);
        }

        // When
        var it = list.iterator();

        CountDownLatch startModifier = new CountDownLatch(1);
        CountDownLatch iterationComplete = new CountDownLatch(1);

        Thread modifierThread = new Thread(() -> {
            try {
                startModifier.await();
                list.add(100);
                list.remove(Integer.valueOf(0));
                list.add(101);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                iterationComplete.countDown();
            }
        });

        modifierThread.start();
        startModifier.countDown();

        List<Integer> collectedElements = new ArrayList<>();
        while (it.hasNext()) {
            collectedElements.add(it.next());
        }
        iterationComplete.await();
        modifierThread.join();

        // Then
        assertEquals(initialCount, collectedElements.size());
        assertEquals(expectedIteratedElements, collectedElements);

        assertEquals(initialCount + 1, list.size());

        List<Integer> actualModifiedListElements = new ArrayList<>();
        list.iterator().forEachRemaining(actualModifiedListElements::add);

        List<Integer> expectedModifiedListContent = List.of(1, 2, 3, 4, 100, 101);
        assertEquals(expectedModifiedListContent, actualModifiedListElements);
    }
}
