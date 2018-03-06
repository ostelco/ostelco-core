package com.telenordigital.prime.disruptor;

import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClearingEventHandler implements EventHandler<PrimeEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ClearingEventHandler.class);

    @Override
    public void onEvent(
            final PrimeEvent event,
            final long sequence,
            final boolean endOfBatch) {
        try {
            event.clear();
        } catch (Exception e) {
            LOG.warn("Exception clearing the prime event", e);
        }
    }
}
