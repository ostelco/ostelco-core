package com.telenordigital.prime.storage.entities;

import java.util.HashMap;
import java.util.Map;

public interface PurchaseRequest {
    String getSku();

    String getPaymentToken();

    String getMsisdn();

    long getMillisSinceEpoch();

    String getId();

    default Map<String, Object> asMap() {
        final Map<String, Object> result = new HashMap<>();
        result.put("msisdn", getMsisdn());
        result.put("sku", getSku());
        result.put("paymentToken", getPaymentToken());
        return result;
    }
}
