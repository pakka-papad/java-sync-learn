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
        if (parties <= 0) {
            throw new IllegalArgumentException("parties must be positive");
        }
        this.parties = parties;
        this.barrierAction = barrierAction;
        this.gen = new Generation();
        this.lock = new ReentrantLock();
        gateOpen = this.lock.newCondition();
    }

    private void breakGate() {
        gen.isBroken = true;
        waitingParties = 0;
        gateOpen.signalAll();
    }

    private void awaitFirstCheck(Thread currentThread) throws BrokenBarrierException, InterruptedException {
        if (gen.isBroken) {
            throw new BrokenBarrierException();
        }
        if (currentThread.isInterrupted()) {
            currentThread.interrupt();
            breakGate();
            throw new InterruptedException();
        }
    }

    private void performLastThreadAction() {
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

    public int await() throws InterruptedException, BrokenBarrierException {
        var currentThread = Thread.currentThread();
        lock.lock();
        try {
            awaitFirstCheck(currentThread);
            var arrivalIndex = waitingParties;
            waitingParties++;
            boolean isLastThread = waitingParties == parties;
            var myGen = gen;
            if (!isLastThread) {
                while (gen.equals(myGen) && !myGen.isBroken && !currentThread.isInterrupted()) {
                    try {
                        gateOpen.await();
                    } catch (InterruptedException ie) {
                        breakGate();
                        currentThread.interrupt();
                        throw ie;
                    }
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
                performLastThreadAction();
            }
            return arrivalIndex;
        } finally {
            lock.unlock();
        }
    }

    public int await(final long timeout, final TimeUnit unit)
            throws InterruptedException, BrokenBarrierException, TimeoutException {
        var currentThread = Thread.currentThread();
        var waitUpto = System.nanoTime() + unit.toNanos(timeout);
        lock.lock();
        try {
            awaitFirstCheck(currentThread);
            var arrivalIndex = waitingParties;
            waitingParties++;
            boolean isLastThread = waitingParties == parties;
            var myGen = gen;
            if (!isLastThread) {
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
                performLastThreadAction();
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
