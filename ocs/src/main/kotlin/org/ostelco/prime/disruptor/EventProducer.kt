package org.ostelco.prime.disruptor

import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.prime.model.Bundle

interface EventProducer {
    fun topupDataBundleBalanceEvent(
            requestId: String,
            bundleId: String,
            bytes: Long)

    fun releaseReservedDataBucketEvent(
            msisdn: String,
            bytes: Long)
    
    fun injectCreditControlRequestIntoRingbuffer(
            streamId: String,
            request: CreditControlRequestInfo)

    fun addBundle(bundle: Bundle)

    fun addMsisdnToBundleMapping(
            msisdn: String,
            bundleId: String)
}