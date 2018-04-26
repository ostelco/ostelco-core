package org.ostelco.prime.events

import com.google.common.base.Preconditions.checkNotNull
import org.ostelco.prime.logger
import org.ostelco.prime.ocs.OcsState
import org.ostelco.prime.storage.ProductCatalogItem
import org.ostelco.prime.storage.ProductDescriptionCacheImpl
import org.ostelco.prime.storage.PurchaseRequestHandler
import org.ostelco.prime.storage.StorageInitiatedEventExecutor
import org.ostelco.prime.storage.entities.PurchaseRequestImpl
import org.ostelco.prime.storage.entities.Subscriber
import java.time.Instant

// Badly named class
class EventListeners(ocsState: OcsState) {

    private val LOG by logger()

    private val executor: StorageInitiatedEventExecutor = StorageInitiatedEventExecutor()

    private val ocsState: OcsState = checkNotNull(ocsState)

    private val millisSinceEpoch: Long
        get() = Instant.now().toEpochMilli()

    fun loadSubscriberBalanceDataFromFirebaseToInMemoryStructure(
            subscribers: Collection<Subscriber>) {
        LOG.info("Loading initial balance from storage to in-memory OcsState")
        for (subscriber in subscribers) {
            ocsState.injectSubscriberIntoOCS(subscriber)
        }
    }


    fun purchaseRequestListener(key: String, req: PurchaseRequestImpl) {
        req.setId(key)
        req.setMillisSinceEpoch(millisSinceEpoch)
        executor.onPurchaseRequest(req)
        // return null // XXX Hack to satisfy BiFunction's void return type
    }

    fun productCatalogItemListener(item: ProductCatalogItem) {
        val sku = item.sku
        val noOfBytes = item.noOfBytes
        if (sku != null && noOfBytes != null) {
            ProductDescriptionCacheImpl. // XXX This is an awful hack!
                    addTopupProduct(sku, noOfBytes)
        }
    }

    // XXX I don't like this!
    fun addPurchaseRequestHandler(handler: PurchaseRequestHandler) {
        executor.addPurchaseRequestHandler(handler)
    }
}
