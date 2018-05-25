package org.ostelco.prime.ocs

import org.ostelco.prime.disruptor.PrimeEventProducer
import org.ostelco.prime.handler.PurchaseRequestHandler
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.legacy.Storage

class OcsSubscriberServiceImpl : OcsSubscriberService by OcsSubscriberServiceSingleton

object OcsSubscriberServiceSingleton : OcsSubscriberService {

    private lateinit var purchaseRequestHandler: PurchaseRequestHandler

    private val storage by lazy { getResource<Storage>() }

    fun init(producer: PrimeEventProducer) {
        purchaseRequestHandler = PurchaseRequestHandler(producer, storage)
    }

    override fun topup(msisdn: String, sku: String) {
        purchaseRequestHandler.handlePurchaseRequest(msisdn, sku)
    }
}