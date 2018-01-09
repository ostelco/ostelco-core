package com.telenordigital.prime.disruptor;

import com.lmax.disruptor.EventFactory;

/**
 * @author Vihang Patil (vihang.patil@telenordigital.com)
 */
public final class PrimeEventFactory implements EventFactory<PrimeEvent> {

    @Override
    public PrimeEvent newInstance() {
        return new PrimeEvent();
    }
}
