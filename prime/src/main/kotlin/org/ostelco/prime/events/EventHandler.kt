package org.ostelco.prime.events

import org.ostelco.prime.storage.ProductCatalogItem
import org.ostelco.prime.storage.ProductDescriptionCacheImpl
import org.ostelco.prime.storage.PurchaseRequestHandler
import org.ostelco.prime.storage.StorageInitiatedEventExecutor
import org.ostelco.prime.storage.entities.PurchaseRequestImpl
import java.time.Instant

class EventHandler {

    private val executor: StorageInitiatedEventExecutor = StorageInitiatedEventExecutor()

    private val millisSinceEpoch: Long
        get() = Instant.now().toEpochMilli()


    fun purchaseRequestHandler(key: String, req: PurchaseRequestImpl) {
        req.setId(key)
        req.setMillisSinceEpoch(millisSinceEpoch)
        executor.onPurchaseRequest(req)
        // return null // XXX Hack to satisfy BiFunction's void return type
    }

    fun productCatalogItemHandler(item: ProductCatalogItem) {
        val sku = item.sku
        val noOfBytes = item.noOfBytes
        if (sku != null && noOfBytes != null) {
            ProductDescriptionCacheImpl. // XXX This is an awful hack!
                    addTopupProduct(sku, noOfBytes)
        }
    }

    fun addPurchaseRequestHandler(handler: PurchaseRequestHandler) {
        executor.addPurchaseRequestHandler(handler)
    }
}
