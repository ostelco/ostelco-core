package com.telenordigital.prime.disruptor;

import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import io.dropwizard.lifecycle.Managed;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author Vihang Patil <vihang.patil@telenordigital.com>
 */
public final class PrimeDisruptor implements Managed {


    /**
     * Buffer size defaults to 65536 = 2^16
     */
    private static final int BUFFER_SIZE = 65536;

    private static final int TIMEOUT_IN_SECONDS = 10;

    private final Disruptor<PrimeEvent> disruptor;

    public PrimeDisruptor() {
        final ThreadFactory threadFactory = Executors.privilegedThreadFactory();
        this.disruptor = new Disruptor<>(PrimeEvent::new, BUFFER_SIZE, threadFactory);
    }

    @Override
    public void start() {
        disruptor.start();
    }

    @Override
    public void stop() throws TimeoutException {
        disruptor.shutdown(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    public Disruptor<PrimeEvent> getDisruptor() {
        return disruptor;
    }
}
