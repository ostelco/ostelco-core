package org.ostelco.prime.ocs

import arrow.core.Either
import org.ostelco.prime.disruptor.EventProducer
import org.ostelco.prime.handler.OcsStateUpdateHandler
import org.ostelco.prime.handler.PurchaseRequestHandler
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientDataSource

/**
 * This class is using the singleton class as delegate.
 * This is done because the [java.util.ServiceLoader] expects public no-args constructor, which is absent in Singleton.
 */
class OcsPrimeService : OcsSubscriberService by OcsPrimeServiceSingleton, OcsAdminService by OcsPrimeServiceSingleton

object OcsPrimeServiceSingleton : OcsSubscriberService, OcsAdminService {

    lateinit var purchaseRequestHandler: PurchaseRequestHandler
    private lateinit var ocsStateUpdateHandler: OcsStateUpdateHandler

    private val storage by lazy { getResource<ClientDataSource>() }

    fun init(producer: EventProducer) {
        purchaseRequestHandler = PurchaseRequestHandler(producer, storage)
        ocsStateUpdateHandler = OcsStateUpdateHandler(producer)
    }

    override fun topup(subscriberId: String, sku: String): Either<String, Unit> {
        return purchaseRequestHandler.handlePurchaseRequest(subscriberId, sku)
    }

    override fun addBundle(bundle: Bundle) {
        ocsStateUpdateHandler.addBundle(bundle)
    }

    override fun addMsisdnToBundleMapping(msisdn: String, bundleId: String) {
        ocsStateUpdateHandler.addMsisdnToBundleMapping(msisdn, bundleId)
    }
}