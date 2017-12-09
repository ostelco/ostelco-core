package com.telenordigital.prime.firebase;

import com.telenordigital.prime.events.AsMappable;
import com.telenordigital.prime.events.RecordOfPurchase;

import java.util.HashMap;
import java.util.Map;

import static com.google.api.client.util.Preconditions.checkNotNull;

public final class FbRecordOfPurchase implements RecordOfPurchase, AsMappable {

    private  final String msisdn;

    private  final String sku;

    private  final long millisSinceEpoch;

    public FbRecordOfPurchase(
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

    @Override
    public Map<String, Object> asMap() {
        final Map<String, Object> result = new HashMap<>();
        result.put("msisdn", msisdn);
        result.put("sku", sku);
        result.put("timeInMillisSinceEpoch", millisSinceEpoch);
        return result;
    }
}
