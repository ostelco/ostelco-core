package com.telenordigital.prime.events;

import com.telenordigital.prime.disruptor.PrimeEventProducer;

public final class OcsBalanceUpdaterImpl implements OcsBalanceUpdater {
    private final PrimeEventProducer producer;

    public OcsBalanceUpdaterImpl(final PrimeEventProducer producer) {
        this.producer = checkNotNull(producer);
    }


    @Override
    public void updateBalance(final String msisdn, final long noOfBytesToTopUp) {
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
