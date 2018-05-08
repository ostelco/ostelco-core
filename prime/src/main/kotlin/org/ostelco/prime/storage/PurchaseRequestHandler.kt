package org.ostelco.prime.storage

import org.ostelco.prime.model.PurchaseRequest

interface PurchaseRequestHandler {
    fun onPurchaseRequest(request: PurchaseRequest)
}
