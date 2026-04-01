package org.example.exchanger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Exchanger<T> {

    private static final int NO_PARTICIPANTS = 0;
    private static final int ONE_PARTICIPANT = 1;
    private static final int EXCHANGE = 2;

    private Generation gen = new Generation();

    private static class Generation {
        private int state = NO_PARTICIPANTS;
        private final Object[] data = new Object[2];
    }

    public synchronized T exchange(T data) throws InterruptedException {
        if (gen.state == NO_PARTICIPANTS) {
            try {
                return stateNoParticipants(false, 0, data);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        } else if (gen.state == ONE_PARTICIPANT) {
            return stateOneParticipants(false, 0, data);
        }
        return null;
    }

    public synchronized T tryExchange(long timeout, TimeUnit unit, T data)
            throws InterruptedException, TimeoutException {
        final var waitUpto = unit.toNanos(timeout) + System.nanoTime();
        if (gen.state == NO_PARTICIPANTS) {
            return stateNoParticipants(true, waitUpto, data);
        } else if (gen.state == ONE_PARTICIPANT) {
            return stateOneParticipants(true, waitUpto, data);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private synchronized T stateNoParticipants(boolean hasTimeOut, long waitUptoNanos, T data)
            throws InterruptedException, TimeoutException {
        final var myGen = gen;
        myGen.state = ONE_PARTICIPANT;
        myGen.data[0] = data;
        try {
            while (myGen.state != EXCHANGE) {
                if (hasTimeOut) {
                    final var waitMoreNano = waitUptoNanos - System.nanoTime();
                    if (waitMoreNano <= 0) {
                        throw new TimeoutException();
                    }
                    final var waitMoreMilli = TimeUnit.NANOSECONDS.toMillis(waitMoreNano);
                    final var extraNano = waitMoreNano - TimeUnit.MILLISECONDS.toNanos(waitMoreMilli);
                    wait(waitMoreMilli, (int) extraNano);
                } else {
                    wait();
                }
            }
        } finally {
            if (myGen.state != EXCHANGE) {
                gen = new Generation();
            }
        }
        return (T) myGen.data[1];
    }

    @SuppressWarnings("unchecked")
    private synchronized T stateOneParticipants(boolean hasTimeOut, long waitUptoNanos, T data) {
        final var myGen = gen;
        myGen.state = EXCHANGE;
        myGen.data[1] = data;
        gen = new Generation();
        notifyAll();
        return (T) myGen.data[0];
    }
}
