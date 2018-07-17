package org.ostelco.prime.ocs

import org.ostelco.prime.disruptor.PrimeEventProducer
import org.ostelco.prime.handler.PurchaseRequestHandler
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientDataSource

/**
 * This class is using the singleton class as delegate.
 * This is done because the {@link java.util.ServiceLoader} expects public no-args constructor, which is absent in Singleton.
 */
class OcsSubscriberServiceImpl : OcsSubscriberService by OcsSubscriberServiceSingleton

object OcsSubscriberServiceSingleton : OcsSubscriberService {

    private lateinit var purchaseRequestHandler: PurchaseRequestHandler

    private val storage by lazy { getResource<ClientDataSource>() }

    fun init(producer: PrimeEventProducer) {
        purchaseRequestHandler = PurchaseRequestHandler(producer, storage)
    }

    override fun topup(msisdn: String, sku: String) {
        purchaseRequestHandler.handlePurchaseRequest(msisdn, sku)
    }
}