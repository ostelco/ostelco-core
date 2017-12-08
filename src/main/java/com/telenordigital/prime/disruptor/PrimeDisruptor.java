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

    private final Disruptor<PrimeEvent> disruptor;

    public PrimeDisruptor() {
        ThreadFactory threadFactory = Executors.privilegedThreadFactory();
        int bufferSize = 65536;
        disruptor = new Disruptor<>(PrimeEvent::new, bufferSize, threadFactory);
    }

    @Override
    public void start() {
        disruptor.start();
    }

    @Override
    public void stop() throws TimeoutException {
        disruptor.shutdown(10, TimeUnit.SECONDS);
    }

    public Disruptor<PrimeEvent> getDisruptor() {
        return disruptor;
    }
}
