package org.example.rwlock;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class ReentrantRWLock implements ReadWriteLock {

    private final ReadLock rLock;
    private final WriteLock wLock;

    private final Queue<Thread> writers = new LinkedList<>();
    private final Map<Thread, Integer> readerHoldCount = new HashMap<>();
    private Thread currentWriter;
    private int currentWriterHoldCount;


    ReentrantRWLock() {
        rLock = new ReadLock(this);
        wLock = new WriteLock(this);
        currentWriter = null;
        currentWriterHoldCount = 0;
    }

    @Override
    public Lock readLock() {
        return rLock;
    }

    @Override
    public Lock writeLock() {
        return wLock;
    }

    public int getReadHoldCount() {
        var currentThread = Thread.currentThread();
        synchronized (this) {
            return readerHoldCount.getOrDefault(currentThread, 0);
        }
    }

    public int getWriteHoldCount() {
        var currentThread = Thread.currentThread();
        synchronized (this) {
            return (Objects.equals(currentThread, currentWriter) ? currentWriterHoldCount : 0);
        }
    }

    private class ReadLock implements Lock {

        private final ReentrantRWLock lock;

        private ReadLock(ReentrantRWLock lock) {
            this.lock = lock;
        }

        private boolean canBecomeReader(Thread currentThread) {
            if (Objects.equals(currentThread, lock.currentWriter)) {
                // To support lock downgrade
                return true;
            }
            return lock.currentWriter == null && lock.writers.isEmpty();
        }

        @Override
        public void lock() {
            final var currentThread = Thread.currentThread();
            synchronized (lock) {
                boolean interrupted = false;
                while (!canBecomeReader(currentThread)) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    currentThread.interrupt();
                }
                lock.readerHoldCount.put(currentThread, lock.readerHoldCount.getOrDefault(currentThread, 0) + 1);
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            final var currentThread = Thread.currentThread();
            synchronized (lock) {
                while (!canBecomeReader(currentThread)) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ie) {
                        throw new InterruptedException(ie.getMessage());
                    }
                }
                lock.readerHoldCount.put(currentThread, lock.readerHoldCount.getOrDefault(currentThread, 0) + 1);
            }
        }

        @Override
        public boolean tryLock() {
            final var currentThread = Thread.currentThread();
            synchronized (lock) {
                if (!canBecomeReader(currentThread)) {
                    return false;
                }
                lock.readerHoldCount.put(currentThread, lock.readerHoldCount.getOrDefault(currentThread, 0) + 1);
                return true;
            }
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            final var currentThread = Thread.currentThread();
            var waitUptoNano = unit.toNanos(time) + System.nanoTime();
            synchronized (lock) {
                while (!canBecomeReader(currentThread)) {
                    var waitForNano = waitUptoNano - System.nanoTime();
                    var waitForMilli = TimeUnit.NANOSECONDS.toMillis(waitForNano);
                    if (waitForMilli <= 0) {
                        return false;
                    }
                    try {
                        lock.wait(waitForMilli);
                    } catch (InterruptedException ie) {
                        throw new InterruptedException(ie.getMessage());
                    }
                }
                lock.readerHoldCount.put(currentThread, lock.readerHoldCount.getOrDefault(currentThread, 0) + 1);
                return true;
            }
        }

        @Override
        public void unlock() {
            final var currentThread = Thread.currentThread();
            synchronized (lock) {
                var currentHoldCount = lock.readerHoldCount.get(currentThread);
                if (currentHoldCount == null) {
                    return;
                }
                if (currentHoldCount == 1) {
                    lock.readerHoldCount.remove(currentThread);
                } else {
                    lock.readerHoldCount.put(currentThread, currentHoldCount - 1);
                }
                if (lock.readerHoldCount.isEmpty()) {
                    lock.notifyAll();
                }
            }
        }

        @Override
        public Condition newCondition() {
            return null;
        }
    }

    private class WriteLock implements Lock {

        private final ReentrantRWLock lock;

        WriteLock(ReentrantRWLock lock) {
            this.lock = lock;
        }

        private boolean isCurrentThreadAtHead(Thread currentThread) {
            return Objects.equals(currentThread, lock.writers.peek());
        }

        private boolean canBecomeWriter(Thread currentThread) {
            return ((isCurrentThreadAtHead(currentThread) && lock.currentWriter == null && readerHoldCount.isEmpty()) ||
                    (lock.currentWriter == currentThread));
        }

        @Override
        public void lock() {
            synchronized (lock) {
                var currentThread = Thread.currentThread();
                if (lock.currentWriter == currentThread) {
                    lock.currentWriterHoldCount++;
                    return;
                }
                lock.writers.add(currentThread);
                boolean interrupted = false;
                while (!canBecomeWriter(currentThread)) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ie) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    currentThread.interrupt();
                }
                if (lock.currentWriter == currentThread) {
                    lock.currentWriterHoldCount++;
                    return;
                }
                // currentThread is at head, lock.currentWriter == null holds, lock.readerHoldCount is empty
                lock.writers.poll();
                lock.currentWriter = currentThread;
                lock.currentWriterHoldCount = 1;
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            synchronized (lock) {
                var currentThread = Thread.currentThread();
                if (lock.currentWriter == currentThread) {
                    lock.currentWriterHoldCount++;
                    return;
                }
                lock.writers.add(currentThread);
                while (!canBecomeWriter(currentThread)) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ie) {
                        lock.writers.remove(currentThread);
                        throw new InterruptedException(ie.getMessage());
                    }
                }
                if (lock.currentWriter == currentThread) {
                    lock.currentWriterHoldCount++;
                    return;
                }
                // currentThread is at head, lock.currentWriter == null holds, lock.readerHoldCount is empty
                lock.writers.poll();
                lock.currentWriter = currentThread;
                lock.currentWriterHoldCount = 1;
            }
        }

        @Override
        public boolean tryLock() {
            synchronized (lock) {
                var currentThread = Thread.currentThread();
                if (lock.currentWriter == currentThread) {
                    lock.currentWriterHoldCount++;
                    return true;
                }
                if (lock.currentWriter == null && lock.writers.isEmpty() && lock.readerHoldCount.isEmpty()) {
                    lock.currentWriter = currentThread;
                    lock.currentWriterHoldCount = 1;
                    return true;
                }
                return false;
            }
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            var waitUptoNano = unit.toNanos(time) + System.nanoTime();
            synchronized (lock) {
                var currentThread = Thread.currentThread();
                if (lock.currentWriter == currentThread) {
                    lock.currentWriterHoldCount++;
                    return true;
                }
                writers.add(currentThread);
                while (!canBecomeWriter(currentThread)) {
                    var waitForNano = waitUptoNano - System.nanoTime();
                    var waitForMilli = TimeUnit.NANOSECONDS.toMillis(waitForNano);
                    if (waitForMilli <= 0) {
                        lock.writers.remove(currentThread);
                        return false;
                    }
                    try {
                        lock.wait(waitForMilli);
                    } catch (InterruptedException ie) {
                        lock.writers.remove(currentThread);
                        throw new InterruptedException(ie.getMessage());
                    }
                }
                if (lock.currentWriter == currentThread) {
                    lock.currentWriterHoldCount++;
                    return true;
                }
                // currentThread is at head, lock.currentWriter == null holds, lock.readerHoldCount is empty
                lock.writers.poll();
                lock.currentWriter = currentThread;
                lock.currentWriterHoldCount = 1;
                return true;
            }
        }

        @Override
        public void unlock() {
            synchronized (lock) {
                var currentThread = Thread.currentThread();
                if (lock.currentWriter != currentThread) {
                    return;
                }
                lock.currentWriterHoldCount--;
                if (lock.currentWriterHoldCount == 0) {
                    lock.currentWriter = null;
                    lock.notifyAll();
                }
            }
        }

        @Override
        public Condition newCondition() {
            return null;
        }
    }
}
