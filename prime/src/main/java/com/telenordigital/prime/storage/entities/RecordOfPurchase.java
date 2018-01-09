package com.telenordigital.prime.storage.entities;

import com.telenordigital.prime.storage.AsMappable;

import java.util.HashMap;
import java.util.Map;

public interface RecordOfPurchase extends AsMappable {
    String getMsisdn();

    long   getMillisSinceEpoch();

    String getSku();

    @Override
    default Map<String, Object> asMap() {
        final Map<String, Object> result = new HashMap<>();
        result.put("msisdn", getMsisdn());
        result.put("sku", getSku());
        result.put("timeInMillisSinceEpoch", getMillisSinceEpoch());
        return result;
    }
}
