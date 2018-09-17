package org.ostelco.prime.handler

import arrow.core.getOrElse
import org.ostelco.prime.disruptor.EventProducer
import org.ostelco.prime.logger
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientGraphStore

class PurchaseRequestHandler(
        private val producer: EventProducer,
        private val storage: ClientGraphStore = getResource()) {

    private val logger by logger()

    fun handlePurchaseRequest(
            subscriberId: String,
            productSku: String) {

        logger.info("Handling purchase request - subscriberId: {} sku = {}", subscriberId, productSku)

        // get Product by SKU
        storage.getProduct(subscriberId, productSku)
                .map { product ->
                    val noOfBytes = product.properties["noOfBytes"]?.replace("_", "")?.toLong()

                    val bundleId = storage.getBundles(subscriberId).map { it?.first()?.id }.getOrElse { null }

                    if (bundleId != null && noOfBytes != null && noOfBytes > 0) {

                        logger.info("Handling topup product - bundleId: {} topup: {}", bundleId, noOfBytes)

                        producer.topupDataBundleBalanceEvent(bundleId, noOfBytes)
                    }
                }.mapLeft {
                    // TODO vihang: instead of throwing exception, return arrow.Either.left
                    throw Exception("Not a valid SKU: $productSku")
                }
    }
}
