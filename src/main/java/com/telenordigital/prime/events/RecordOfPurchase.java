package com.telenordigital.prime.events;

public interface RecordOfPurchase {
    String getMsisdn();

    long   getMillisSinceEpoch();

    String getSku();
}
