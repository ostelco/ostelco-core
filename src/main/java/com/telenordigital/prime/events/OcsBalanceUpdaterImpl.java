package com.telenordigital.prime.events;

import com.telenordigital.prime.disruptor.PrimeEventProducer;

import static com.google.common.base.Preconditions.checkNotNull;

public final class OcsBalanceUpdaterImpl implements OcsBalanceUpdater {
    private final PrimeEventProducer producer;

    public OcsBalanceUpdaterImpl(final PrimeEventProducer producer) {
        this.producer = checkNotNull(producer);
    }


    @Override
    public final void updateBalance(final String msisdn, long noOfBytesToTopUp) {
        // XXX removing '+' if it exists
        // XXX Use rewriting functions directly

        final String sanitizedMsisdn;
        if (msisdn.charAt(0) == '+') {
            sanitizedMsisdn = msisdn.substring(1);
        } else {
            sanitizedMsisdn = msisdn;
        }
        producer.topupDataBundleBalanceEvent(
                sanitizedMsisdn,
                noOfBytesToTopUp);
    }
}
