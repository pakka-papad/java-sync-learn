package org.example.rwlock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ReentrantRWLockTest {

    @Test
    @Timeout(5)
    void readLock_allowsMultipleConcurrentReaders() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();

        // When
        final var maxReadLocks = new AtomicInteger(0);
        final var readerCount = 6;
        final var lockCountUpdate = new CountDownLatch(readerCount);
        final var lockCountCheck = new CountDownLatch(1);
        final var done = new CountDownLatch(readerCount);
        Runnable readerAction = () -> {
            lock.readLock().lock();
            maxReadLocks.addAndGet(lock.getReadHoldCount());
            lockCountUpdate.countDown();
            try {
                lockCountCheck.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.readLock().unlock();
            done.countDown();
        };
        for (int i = 0; i < readerCount; i++) {
            var thread = new Thread(readerAction);
            thread.start();
        }

        // Then
        lockCountUpdate.await();
        assertEquals(readerCount, maxReadLocks.get());
        lockCountCheck.countDown();
        done.await();
    }

    @Test
    @Timeout(5)
    void writeLock_isExclusive() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();

        // When
        final var maxWriteLocks = new AtomicInteger(0);
        final var writerCount = 6;
        final var lockCountUpdate = new CountDownLatch(1);
        final var lockCountCheck = new CountDownLatch(1);
        final var done = new CountDownLatch(writerCount);
        Runnable readerAction = () -> {
            lock.writeLock().lock();
            maxWriteLocks.addAndGet(lock.getWriteHoldCount());
            lockCountUpdate.countDown();
            try {
                lockCountCheck.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.writeLock().unlock();
            done.countDown();
        };
        for (int i = 0; i < writerCount; i++) {
            var thread = new Thread(readerAction);
            thread.start();
        }

        // Then
        lockCountUpdate.await();
        assertEquals(1, maxWriteLocks.get());
        lockCountCheck.countDown();
        done.await();
    }

    @Test
    @Timeout(5)
    void writeLock_blocksReaders() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();

        // When
        var writerLocked = new CountDownLatch(1);
        var waitWriter = new CountDownLatch(1);
        Runnable writerAction = () -> {
            lock.writeLock().lock();
            writerLocked.countDown();
            try {
                waitWriter.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.writeLock().unlock();
        };
        var writerThread = new Thread(writerAction);
        writerThread.start();

        // Then
        writerLocked.await();
        assertFalse(lock.readLock().tryLock(600, TimeUnit.MILLISECONDS));
        waitWriter.countDown();
    }

    @Test
    @Timeout(5)
    void readLock_blocksWriters() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();

        // When
        var readerLocked = new CountDownLatch(1);
        var waitReader = new CountDownLatch(1);
        Runnable readerAction = () -> {
            lock.readLock().lock();
            readerLocked.countDown();
            try {
                waitReader.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.readLock().unlock();
        };
        var readerThread = new Thread(readerAction);
        readerThread.start();

        // Then
        readerLocked.await();
        assertFalse(lock.writeLock().tryLock(600, TimeUnit.MILLISECONDS));
        waitReader.countDown();
    }

    @Test
    @Timeout(5)
    void readLock_isReentrant() {
        // Given
        final var lock = new ReentrantRWLock();
        lock.readLock().lock();

        // When
        lock.readLock().lock();

        // Then
        assertEquals(2, lock.getReadHoldCount());
        lock.readLock().unlock();
        assertEquals(1, lock.getReadHoldCount());
        lock.readLock().unlock();
    }

    @Test
    @Timeout(5)
    void writeLock_isReentrant() {
        // Given
        final var lock = new ReentrantRWLock();
        lock.writeLock().lock();

        // When
        lock.writeLock().lock();

        // Then
        assertEquals(2, lock.getWriteHoldCount());
        lock.writeLock().unlock();
        assertEquals(1, lock.getWriteHoldCount());
        lock.writeLock().unlock();
    }
    
    @Test
    @Timeout(5)
    void lockDowngrading_isAllowed() {
        // Given
        final var lock = new ReentrantRWLock();
        lock.writeLock().lock();

        // When
        lock.readLock().lock();

        // Then
        assertEquals(1, lock.getReadHoldCount());
        assertEquals(1, lock.getWriteHoldCount());
    }

    @Test
    @Timeout(5)
    void lockUpgrading_isNotAllowed() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();
        lock.readLock().lock();

        // When & Then
        assertFalse(lock.writeLock().tryLock(600, TimeUnit.MILLISECONDS));
    }

    @Test
    @Timeout(5)
    void writerHasPriorityOverReaders() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();
        lock.readLock().lock();

        // When
        final var writerStart = new CountDownLatch(1);
        final var readerStart = new CountDownLatch(1);
        final var order = new CopyOnWriteArrayList<Integer>();
        final var done = new CountDownLatch(2);
        Runnable writerAction = () -> {
            writerStart.countDown();
            lock.writeLock().lock();
            order.add(1);
            lock.writeLock().unlock();
            done.countDown();
        };
        Runnable readerAction = () -> {
            readerStart.countDown();
            lock.readLock().lock();
            order.add(2);
            lock.readLock().unlock();
            done.countDown();
        };
        var writerThread = new Thread(writerAction);
        writerThread.start();
        writerStart.await();
        var readerThread = new Thread(readerAction);
        readerThread.start();
        readerStart.await();

        // Then
        lock.readLock().unlock();
        done.await();
        assertEquals(2, order.size());
        assertEquals(1, order.get(0));
        assertEquals(2, order.get(1));
    }

    @Test
    @Timeout(5)
    void readLock_lockInterruptibly_throwsInterruptedException() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();
        lock.writeLock().lock();

        // When
        final var startThread = new CountDownLatch(1);
        final var done = new CountDownLatch(1);
        final var thrown = new AtomicBoolean(false);
        Runnable action = () -> {
            startThread.countDown();
            try {
                lock.readLock().lockInterruptibly();
            } catch (InterruptedException ie) {
                thrown.set(true);
            }
            done.countDown();
        };
        var thread = new Thread(action);
        thread.start();

        // Then
        startThread.await();
        thread.interrupt();
        done.await();
        assertTrue(thrown.get());
    }

    @Test
    @Timeout(5)
    void writeLock_lockInterruptibly_throwsInterruptedException() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();
        lock.writeLock().lock();

        // When
        final var startThread = new CountDownLatch(1);
        final var done = new CountDownLatch(1);
        final var thrown = new AtomicBoolean(false);
        Runnable action = () -> {
            startThread.countDown();
            try {
                lock.writeLock().lockInterruptibly();
            } catch (InterruptedException ie) {
                thrown.set(true);
            }
            done.countDown();
        };
        var thread = new Thread(action);
        thread.start();

        // Then
        startThread.await();
        thread.interrupt();
        done.await();
        assertTrue(thrown.get());
    }

    @Test
    @Timeout(5)
    void readLock_lock_handlesInterruption() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();
        lock.writeLock().lock();

        // When
        final var startThread = new CountDownLatch(1);
        final var done = new CountDownLatch(1);
        Runnable action = () -> {
            startThread.countDown();
            lock.readLock().lock();
            done.countDown();
        };
        var thread = new Thread(action);
        thread.start();

        // Then
        startThread.await();
        thread.interrupt();
        lock.writeLock().unlock();
        done.await();
    }

    @Test
    @Timeout(5)
    void writeLock_lock_handlesInterruption() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();
        lock.writeLock().lock();

        // When
        final var startThread = new CountDownLatch(1);
        final var done = new CountDownLatch(1);
        Runnable action = () -> {
            startThread.countDown();
            lock.writeLock().lock();
            done.countDown();
        };
        var thread = new Thread(action);
        thread.start();

        // Then
        startThread.await();
        thread.interrupt();
        lock.writeLock().unlock();
        done.await();
    }

    @Test
    @Timeout(5)
    void readLock_tryLock_acquiresLockWhenFree() {
        // Given
        final var lock = new ReentrantRWLock();

        // When & Then
        assertTrue(lock.readLock().tryLock());
    }

    @Test
    @Timeout(5)
    void readLock_tryLock_failsWhenWriterIsActive() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();

        // When
        final var start = new CountDownLatch(1);
        final var wait = new CountDownLatch(1);
        final var done = new CountDownLatch(1);
        Runnable action = () -> {
            lock.writeLock().lock();
            start.countDown();
            try {
                wait.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.writeLock().unlock();
            done.countDown();
        };
        final var thread = new Thread(action);
        thread.start();
        start.await();

        // Then
        assertFalse(lock.readLock().tryLock());
        wait.countDown();
        done.await();
    }

    @Test
    @Timeout(5)
    void writeLock_tryLock_acquiresLockWhenFree() {
        // Given
        final var lock = new ReentrantRWLock();

        // When & Then
        assertTrue(lock.writeLock().tryLock());
    }

    @Test
    @Timeout(5)
    void writeLock_tryLock_failsWhenReaderIsActive() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();

        // When
        final var start = new CountDownLatch(1);
        final var wait = new CountDownLatch(1);
        final var done = new CountDownLatch(1);
        Runnable action = () -> {
            lock.readLock().lock();
            start.countDown();
            try {
                wait.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.readLock().unlock();
            done.countDown();
        };
        final var thread = new Thread(action);
        thread.start();
        start.await();

        // Then
        assertFalse(lock.writeLock().tryLock());
        wait.countDown();
        done.await();
    }

    @Test
    @Timeout(5)
    void writeLock_tryLock_failsWhenWriterIsActive() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();

        // When
        final var start = new CountDownLatch(1);
        final var wait = new CountDownLatch(1);
        final var done = new CountDownLatch(1);
        Runnable action = () -> {
            lock.writeLock().lock();
            start.countDown();
            try {
                wait.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.writeLock().unlock();
            done.countDown();
        };
        final var thread = new Thread(action);
        thread.start();
        start.await();

        // Then
        assertFalse(lock.writeLock().tryLock());
        wait.countDown();
        done.await();
    }

    @Test
    @Timeout(5)
    void readLock_tryLockWithTimeout_acquiresLockWhenFree() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();

        // When
        final var start = new CountDownLatch(1);
        final var wait = new CountDownLatch(1);
        final var done = new CountDownLatch(1);
        Runnable action = () -> {
            lock.writeLock().lock();
            start.countDown();
            try {
                wait.await();
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.writeLock().unlock();
            done.countDown();
        };
        final var thread = new Thread(action);
        thread.start();
        start.await();

        // Then
        wait.countDown();
        assertTrue(lock.readLock().tryLock(600, TimeUnit.MILLISECONDS));
        done.await();
    }

    @Test
    @Timeout(5)
    void readLock_tryLockWithTimeout_failsOnTimeout() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();

        // When
        final var start = new CountDownLatch(1);
        final var wait = new CountDownLatch(1);
        final var done = new CountDownLatch(1);
        Runnable action = () -> {
            lock.writeLock().lock();
            start.countDown();
            try {
                wait.await();
                Thread.sleep(700);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.writeLock().unlock();
            done.countDown();
        };
        final var thread = new Thread(action);
        thread.start();
        start.await();

        // Then
        wait.countDown();
        assertFalse(lock.readLock().tryLock(600, TimeUnit.MILLISECONDS));
        done.await();
    }
    
    @Test
    @Timeout(5)
    void writeLock_tryLockWithTimeout_acquiresLockWhenFree() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();

        // When
        final var start = new CountDownLatch(1);
        final var wait = new CountDownLatch(1);
        final var done = new CountDownLatch(1);
        Runnable action = () -> {
            lock.writeLock().lock();
            start.countDown();
            try {
                wait.await();
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.writeLock().unlock();
            done.countDown();
        };
        final var thread = new Thread(action);
        thread.start();
        start.await();

        // Then
        wait.countDown();
        assertTrue(lock.writeLock().tryLock(600, TimeUnit.MILLISECONDS));
        done.await();
    }

    @Test
    @Timeout(5)
    void writeLock_tryLockWithTimeout_failsOnTimeout() throws InterruptedException {
        // Given
        final var lock = new ReentrantRWLock();

        // When
        final var start = new CountDownLatch(1);
        final var wait = new CountDownLatch(1);
        final var done = new CountDownLatch(1);
        Runnable action = () -> {
            lock.writeLock().lock();
            start.countDown();
            try {
                wait.await();
                Thread.sleep(700);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.writeLock().unlock();
            done.countDown();
        };
        final var thread = new Thread(action);
        thread.start();
        start.await();

        // Then
        wait.countDown();
        assertFalse(lock.writeLock().tryLock(600, TimeUnit.MILLISECONDS));
        done.await();
    }

    @Test
    @Timeout(5)
    void unlock_withoutHoldingLock_doesNotThrow() {
        // Given
        final var lock = new ReentrantRWLock();

        // When & Then
        assertDoesNotThrow(() -> lock.readLock().unlock());
        assertDoesNotThrow(() -> lock.writeLock().unlock());
    }
}
