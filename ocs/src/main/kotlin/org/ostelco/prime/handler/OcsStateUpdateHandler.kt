package org.ostelco.prime.handler

import org.ostelco.prime.disruptor.EventProducer
import org.ostelco.prime.model.Bundle

class OcsStateUpdateHandler(private val producer: EventProducer) {

    fun addBundle(bundle: Bundle) {
        producer.addBundle(bundle)
    }

    fun addMsisdnToBundleMapping(msisdn: String, bundleId: String) {
        producer.addMsisdnToBundleMapping(msisdn, bundleId)
    }
}