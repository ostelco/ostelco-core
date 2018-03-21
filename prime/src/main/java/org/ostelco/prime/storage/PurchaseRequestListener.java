package org.ostelco.prime.storage;

import org.ostelco.prime.storage.entities.PurchaseRequest;

public interface PurchaseRequestListener {
    void onPurchaseRequest(PurchaseRequest request);
}
