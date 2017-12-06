package com.telenordigital.prime.events;

import com.telenordigital.prime.disruptor.PrimeEventProducer;

import static com.google.common.base.Preconditions.checkNotNull;

public final class OcsBalanceUpdaterImpl implements OcsBalanceUpdater {
    private final PrimeEventProducer producer;

    public OcsBalanceUpdaterImpl(final PrimeEventProducer producer) {
        this.producer = checkNotNull(producer);
    }


    @Override
    public final void updateBalance(String msisdn, long noOfBytesToTopUp) {
        // XXX removing '+' if it exists
        if (msisdn.charAt(0) == '+') {
            msisdn = msisdn.substring(1);
        }
        producer.topupDataBundleBalanceEvent(
                msisdn,
                noOfBytesToTopUp);
    }
}
