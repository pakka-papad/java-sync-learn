package org.example.cyclicbarrier;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AltCyclicBarrier {

    private static class Generation {
        boolean isBroken;
    }

    private final int parties;
    private final Runnable barrierAction;

    private final Lock lock;
    private final Condition gateOpen;
    private int waitingParties;
    private volatile Generation gen;

    AltCyclicBarrier(final int parties) {
        this(parties, null);
    }

    AltCyclicBarrier(final int parties, final Runnable barrierAction) {
        if (parties <= 0) {
            throw new IllegalArgumentException("parties must be positive");
        }
        this.parties = parties;
        this.barrierAction = barrierAction;
        this.gen = new Generation();
        this.lock = new ReentrantLock();
        gateOpen = this.lock.newCondition();
    }

    private void doBarrierAction() {
        if (barrierAction == null) {
            return;
        }
        try {
            barrierAction.run();
        } catch (Throwable e) {
            breakBarrier();
            throw e;
        }
    }

    private void breakBarrier() {
        lock.lock();
        try {
            waitingParties = 0;
            gen.isBroken = true;
            gateOpen.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void nextGeneration() {
        waitingParties = 0;
        gen = new Generation();
        gateOpen.signalAll();
    }

    public int await() throws InterruptedException, BrokenBarrierException {
        final var currentThread = Thread.currentThread();
        if (currentThread.isInterrupted()) {
            currentThread.interrupt();
            breakBarrier();
            throw new InterruptedException();
        }
        try {
            lock.lockInterruptibly();
            try {
                final var myGen = gen;
                final var arrivalIdx = waitingParties;
                waitingParties++;
                boolean isFinalThread = (waitingParties == parties);
                while (!isFinalThread && gen.equals(myGen) && !myGen.isBroken) {
                    gateOpen.await(); // the outer catch will handle the interruption
                }
                if (myGen.isBroken) {
                    throw new BrokenBarrierException();
                }
                if (isFinalThread) {
                    doBarrierAction();
                    nextGeneration();
                }
                return arrivalIdx;
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            breakBarrier();
            currentThread.interrupt();
            throw new InterruptedException();
        }
    }

    public int await(final long timeout, final TimeUnit unit)
            throws InterruptedException, BrokenBarrierException, TimeoutException {
        final var waitUptoNano = unit.toNanos(timeout) + System.nanoTime();
        final var currentThread = Thread.currentThread();
        if (currentThread.isInterrupted()) {
            currentThread.interrupt();
            breakBarrier();
            throw new InterruptedException();
        }
        try {
            long waitForLock = waitUptoNano - System.nanoTime();
            if (waitForLock <= 0 || !lock.tryLock(waitForLock, TimeUnit.NANOSECONDS)) {
                breakBarrier();
                throw new TimeoutException();
            }
            try {
                final var myGen = gen;
                final var arrivalIdx = waitingParties;
                waitingParties++;
                boolean isFinalThread = (waitingParties == parties);
                while (!isFinalThread && gen.equals(myGen) && !myGen.isBroken) {
                    long waitMore = waitUptoNano - System.nanoTime();
                    // the outer catch will handle the interruption
                    if (waitMore <= 0 || !gateOpen.await(waitMore, TimeUnit.NANOSECONDS)) {
                        breakBarrier();
                        throw new TimeoutException();
                    }
                }
                if (myGen.isBroken) {
                    throw new BrokenBarrierException();
                }
                if (isFinalThread) {
                    doBarrierAction();
                    nextGeneration();
                }
                return arrivalIdx;
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            breakBarrier();
            currentThread.interrupt();
            throw new InterruptedException();
        }
    }

    public int getParties() {
        return parties;
    }

    public void reset() {
        lock.lock();
        try {
            breakBarrier();
            gen = new Generation();
        } finally {
            lock.unlock();
        }
    }

    public boolean isBroken() {
        lock.lock();
        try {
            return gen.isBroken;
        } finally {
            lock.unlock();
        }
    }

    public int getNumberWaiting() {
        lock.lock();
        try {
            return waitingParties;
        } finally {
            lock.unlock();
        }
    }
}
