package org.ostelco.prime.handler

import org.ostelco.prime.disruptor.PrimeEventProducer
import org.ostelco.prime.events.EventProcessorException
import org.ostelco.prime.logger
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.legacy.Storage
import org.ostelco.prime.storage.legacy.StorageException
import java.time.Instant

class PurchaseRequestHandler(
        private val producer: PrimeEventProducer,
        private val storage: Storage = getResource()) {

    private val LOG by logger()

    @Throws(EventProcessorException::class)
    fun handlePurchaseRequest(msisdn: String, sku: String) {

        LOG.info("Handling purchase request - msisdn: {} sku = {}", msisdn, sku)

        // get Product by SKU
        val product = storage.getProduct(sku) ?: throw EventProcessorException("Not a valid SKU: $sku")

        val noOfBytes = product.properties["noOfBytes"]?.toLong()

        if (noOfBytes != null && noOfBytes > 0) {

            LOG.info("Handling topup product - msisdn: {} sku: {} topup: {}", msisdn, sku, noOfBytes)

            val purchaseRecord = PurchaseRecord(
                    msisdn = msisdn,
                    sku = sku,
                    millisSinceEpoch = Instant.now().toEpochMilli())
            try {
                storage.addPurchaseRecord(purchaseRecord)
            } catch (e: StorageException) {
                throw EventProcessorException(e)
            }

            producer.topupDataBundleBalanceEvent(msisdn, noOfBytes)
        }
    }
}
