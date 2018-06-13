package org.ostelco.prime.disruptor

import org.ostelco.ocs.api.CreditControlRequestInfo

interface PrimeEventProducer {
    fun topupDataBundleBalanceEvent(
            msisdn: String,
            bytes: Long)

    fun releaseReservedDataBucketEvent(
            msisdn: String,
            bytes: Long)
    
    fun injectCreditControlRequestIntoRingbuffer(
            request: CreditControlRequestInfo,
            streamId: String)
}