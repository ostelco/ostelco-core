package org.ostelco.prime.storage.legacy

import org.ostelco.prime.model.PurchaseRequest

interface PurchaseRequestHandler {
    fun onPurchaseRequest(request: PurchaseRequest)
}
