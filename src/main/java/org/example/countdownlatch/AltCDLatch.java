package org.example.countdownlatch;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AltCDLatch {

    private volatile int count;
    private final ReentrantLock lock;
    private final Condition isOpen;

    AltCDLatch(final int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be greater than or equal to 0");
        }
        this.count = count;
        lock = new ReentrantLock();
        isOpen = lock.newCondition();
    }

    public void await() throws InterruptedException {
        if (getCount() <= 0) {
            return;
        }
        lock.lockInterruptibly();
        try {
            while (count > 0) {
                isOpen.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        if (getCount() <= 0) {
            return true;
        }
        long waitUptoNano = unit.toNanos(timeout) + System.nanoTime();
        if (!lock.tryLock(waitUptoNano - System.nanoTime(), TimeUnit.NANOSECONDS)) {
            return (getCount() <= 0);
        }
        try {
            while (count > 0 && System.nanoTime() < waitUptoNano) {
                long waitMore = waitUptoNano - System.nanoTime();
                if (!isOpen.await(waitMore, TimeUnit.NANOSECONDS)) {
                    return (getCount() <= 0);
                }
            }
        } finally {
            lock.unlock();
        }
        return true;
    }

    public void countDown() {
        lock.lock();
        try {
            if (count > 0) {
                count--;
                if (count <= 0) {
                    isOpen.signalAll();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public int getCount() {
        return count;
    }
}
