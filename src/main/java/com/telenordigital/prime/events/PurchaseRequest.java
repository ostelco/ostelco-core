package com.telenordigital.prime.events;

public interface PurchaseRequest {
    String getSku();

    String getPaymentToken();

    String getMsisdn();

    long getMillisSinceEpoch();

    String getId();
}
