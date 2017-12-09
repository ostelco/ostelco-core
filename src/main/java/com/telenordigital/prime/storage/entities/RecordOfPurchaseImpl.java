package com.telenordigital.prime.storage.entities;

import static com.google.api.client.util.Preconditions.checkNotNull;

public final class RecordOfPurchaseImpl implements RecordOfPurchase {

    private  final String msisdn;

    private  final String sku;

    private  final long millisSinceEpoch;

    public RecordOfPurchaseImpl(
            final String msisdn,
            final String sku,
            final long millisSinceEpoch) {
        this.msisdn = checkNotNull(msisdn);
        this.sku = checkNotNull(sku);
        this.millisSinceEpoch = millisSinceEpoch;
    }

    @Override
    public String getMsisdn() {
        return msisdn;
    }

    @Override
    public long getMillisSinceEpoch() {
        return millisSinceEpoch;
    }

    @Override
    public String getSku() {
        return sku;
    }
}
