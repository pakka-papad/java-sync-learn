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
    private volatile int waitingParties;
    private volatile Generation gen;

    AltCyclicBarrier(final int parties) {
        this(parties, null);
    }

    AltCyclicBarrier(final int parties, final Runnable barrierAction) {
        this.parties = parties;
        this.barrierAction = barrierAction;
        this.gen = new Generation();
        this.lock = new ReentrantLock();
        gateOpen = this.lock.newCondition();
    }

    private void breakGate() {
        gen.isBroken = true;
        waitingParties = 0;
        gen = new Generation();
        gateOpen.signalAll();
    }

    public int await() throws InterruptedException, BrokenBarrierException {
        if (gen.isBroken) {
            throw new BrokenBarrierException();
        }
        var currentThread = Thread.currentThread();
        if (currentThread.isInterrupted()) {
            currentThread.interrupt();
            lock.lock();
            try {
                breakGate();
            } finally {
                lock.unlock();
            }
            throw new InterruptedException();
        }
        lock.lock();
        try {
            var arrivalIndex = waitingParties;
            waitingParties++;
            boolean isLastThread = waitingParties == parties;
            var myGen = gen;
            while (gen.equals(myGen) && !myGen.isBroken && !currentThread.isInterrupted()) {
                try {
                    gateOpen.await();
                } catch (InterruptedException ie) {
                    breakGate();
                    currentThread.interrupt();
                    throw ie;
                }
            }
            if (myGen.isBroken) {
                throw new BrokenBarrierException();
            }
            if (currentThread.isInterrupted() && gen.equals(myGen)) {
                breakGate();
                throw new InterruptedException();
            }
            if (isLastThread) {
                if (barrierAction != null) {
                    try {
                        barrierAction.run();
                    } catch (Exception e) {
                        breakGate();
                        throw new RuntimeException(e);
                    }
                }
                waitingParties = 0;
                gen = new Generation();
                gateOpen.signalAll();
            }
            return arrivalIndex;
        } finally {
            lock.unlock();
        }
    }

    public int await(final long timeout, final TimeUnit unit)
            throws InterruptedException, BrokenBarrierException, TimeoutException {
        if (gen.isBroken) {
            throw new BrokenBarrierException();
        }
        var currentThread = Thread.currentThread();
        if (currentThread.isInterrupted()) {
            currentThread.interrupt();
            lock.lock();
            try {
                breakGate();
            } finally {
                lock.unlock();
            }
            throw new InterruptedException();
        }
        var waitUpto = System.nanoTime() + unit.toNanos(timeout);
        lock.lock();
        try {
            var arrivalIndex = waitingParties;
            waitingParties++;
            boolean isLastThread = waitingParties == parties;
            var myGen = gen;
            while (gen.equals(myGen) && !myGen.isBroken && !currentThread.isInterrupted() &&
                    System.nanoTime() < waitUpto) {
                var waitMore = waitUpto - System.nanoTime();
                if (waitMore <= 0) {
                    breakGate();
                    throw new TimeoutException();
                }
                try {
                    gateOpen.await(waitMore, TimeUnit.NANOSECONDS);
                } catch (InterruptedException ie) {
                    breakGate();
                    currentThread.interrupt();
                    throw ie;
                }
            }
            if (myGen.isBroken) {
                throw new BrokenBarrierException();
            }
            if (System.nanoTime() > waitUpto && gen.equals(myGen)) {
                breakGate();
                throw new TimeoutException();
            }
            if (currentThread.isInterrupted() && gen.equals(myGen)) {
                breakGate();
                throw new InterruptedException();
            }
            if (isLastThread) {
                if (barrierAction != null) {
                    try {
                        barrierAction.run();
                    } catch (Exception e) {
                        breakGate();
                        throw new RuntimeException(e);
                    }
                }
                waitingParties = 0;
                gen = new Generation();
                gateOpen.signalAll();
            }
            return arrivalIndex;
        } finally {
            lock.unlock();
        }
    }

    public int getParties() {
        return parties;
    }

    public void reset() {
        lock.lock();
        try {
            breakGate();
        } finally {
            lock.unlock();
        }
    }

    public boolean isBroken() {
        return gen.isBroken;
    }

    public int getNumberWaiting() {
        return waitingParties;
    }
}
