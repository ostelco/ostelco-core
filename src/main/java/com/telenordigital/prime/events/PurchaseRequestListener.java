package com.telenordigital.prime.events;

import com.telenordigital.prime.events.PurchaseRequest;

public interface PurchaseRequestListener {
    public void onPurchaseRequest(PurchaseRequest request);
}
