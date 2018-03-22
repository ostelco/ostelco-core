package org.ostelco.prime.storage

import org.ostelco.prime.storage.entities.PurchaseRequest

interface PurchaseRequestListener {
    fun onPurchaseRequest(request: PurchaseRequest)
}
