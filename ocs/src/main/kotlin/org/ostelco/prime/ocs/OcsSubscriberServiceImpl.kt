package org.ostelco.prime.ocs

import org.ostelco.prime.disruptor.PrimeEventProducer
import org.ostelco.prime.handler.PurchaseRequestHandler
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.legacy.Storage

class OcsSubscriberServiceImpl : OcsSubscriberService by OcsSubscriberServiceSingleton

object OcsSubscriberServiceSingleton : OcsSubscriberService {

    private lateinit var purchaseRequestHandler: PurchaseRequestHandler
    private lateinit var producer: PrimeEventProducer
    private lateinit var ocsState: OcsState

    private val storage by lazy { getResource<Storage>() }

    fun init(producer: PrimeEventProducer,
             ocsState: OcsState) {
        this.producer = producer
        this.ocsState = ocsState
        purchaseRequestHandler = PurchaseRequestHandler(producer, storage)
    }

    override fun getBalance(msisdn: String) : Long {
        return ocsState.getDataBundleBytes(msisdn)
    }

    override fun topup(msisdn: String, sku: String) {
        purchaseRequestHandler.handlePurchaseRequest(msisdn, sku)
    }
}