package org.ostelco.prime.handler

import arrow.core.Either
import arrow.core.flatMap
import com.lmax.disruptor.EventHandler
import org.ostelco.prime.disruptor.EventMessageType.TOPUP_DATA_BUNDLE_BALANCE
import org.ostelco.prime.disruptor.EventProducer
import org.ostelco.prime.disruptor.OcsEvent
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientGraphStore
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS

class PurchaseRequestHandler(
        private val producer: EventProducer,
        private val storage: ClientGraphStore = getResource()) : EventHandler<OcsEvent> {

    private val logger by getLogger()

    private val requestMap = ConcurrentHashMap<String, CompletableFuture<String>>()

    fun handlePurchaseRequest(
            subscriberId: String,
            productSku: String): Either<String, Unit> {

        logger.info("Handling purchase request - subscriberId: {} sku = {}", subscriberId, productSku)

        // get Product by SKU
        return storage.getProduct(subscriberId, productSku)
                // if left, map StoreError to String
                .mapLeft {
                    "Unable to Topup. Not a valid SKU: $productSku. ${it.message}"
                }
                // map product to noOfBytes
                .flatMap { product ->
                    val noOfBytes: Long? = product.properties["noOfBytes"]
                            ?.replace("_", "")
                            ?.toLong()
                    if (noOfBytes != null && noOfBytes > 0) {
                        Either.right(noOfBytes)
                    } else {
                        Either.left("Unable to Topup. No bytes to topup for product: $productSku")
                    }
                }
                // map noOfBytes to (noOfBytes, bundleId)
                .flatMap { noOfBytes ->
                    storage.getBundles(subscriberId)
                            .mapLeft { "Unable to Topup. No bundles found for subscriberId: $subscriberId" }
                            .flatMap { bundles ->
                                bundles.firstOrNull()
                                        ?.id
                                        ?.let { Either.right(Pair(noOfBytes, it)) }
                                        ?: Either.left("Unable to Topup. No bundles or invalid bundle found for subscriberId: $subscriberId")
                            }
                }
                .flatMap { (noOfBytes, bundleId) ->
                    logger.info("Handling topup product - bundleId: {} topup: {}", bundleId, noOfBytes)
                    topup(bundleId = bundleId, noOfBytes = noOfBytes)
                }
    }

    override fun onEvent(
            event: OcsEvent,
            sequence: Long,
            endOfBatch: Boolean) {

        if (event.messageType == TOPUP_DATA_BUNDLE_BALANCE) {
            val topupContext = event.topupContext
            if (topupContext != null) {
                requestMap[topupContext.requestId]?.complete(topupContext.errorMessage)
            }
        }
    }

    private fun topup(bundleId: String, noOfBytes: Long): Either<String, Unit> {
        val requestId = UUID.randomUUID().toString()
        val future = CompletableFuture<String>()
        requestMap[requestId] = future
        producer.topupDataBundleBalanceEvent(requestId = requestId, bundleId = bundleId, bytes = noOfBytes)
        val error = future.get(100, MILLISECONDS)
        if (error.isNotBlank()) {
            return Either.left(error)
        }
        return Either.right(Unit)
    }
}
