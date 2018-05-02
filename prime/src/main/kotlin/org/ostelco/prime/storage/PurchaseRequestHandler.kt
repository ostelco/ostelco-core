package org.ostelco.prime.storage

import org.ostelco.prime.storage.entities.PurchaseRequest

interface PurchaseRequestHandler {
    fun onPurchaseRequest(request: PurchaseRequest)
}
