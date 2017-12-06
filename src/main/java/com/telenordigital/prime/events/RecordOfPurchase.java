package com.telenordigital.prime.events;

public interface RecordOfPurchase {
    public String getMsisdn();
    public long    getMillisSinceEpoch();
    public String getSku();
}
