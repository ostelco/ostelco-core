package com.telenordigital.prime.disruptor;

import com.lmax.disruptor.EventFactory;

public final class PrimeEventFactory implements EventFactory<PrimeEvent> {

    @Override
    public PrimeEvent newInstance() {
        return new PrimeEvent();
    }
}
