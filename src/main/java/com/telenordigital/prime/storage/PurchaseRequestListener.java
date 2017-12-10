package com.telenordigital.prime.storage;


import com.telenordigital.prime.storage.entities.PurchaseRequest;

public interface PurchaseRequestListener {
    void onPurchaseRequest(final PurchaseRequest request);
}
