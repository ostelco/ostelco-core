package com.telenordigital.prime.events;

public interface OcsBalanceUpdater {
    void updateBalance(String msisdn, long noOfBytesToTopUp);
}
