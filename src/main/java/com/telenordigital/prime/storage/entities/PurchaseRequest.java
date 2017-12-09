package com.telenordigital.prime.storage.entities;

public interface PurchaseRequest {
    String getSku();

    String getPaymentToken();

    String getMsisdn();

    long getMillisSinceEpoch();

    String getId();
}
