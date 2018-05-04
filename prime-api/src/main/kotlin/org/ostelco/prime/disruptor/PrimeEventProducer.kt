package org.ostelco.prime.disruptor

import org.ostelco.ocs.api.CreditControlRequestInfo

interface PrimeEventProducer {
    fun topupDataBundleBalanceEvent(
            msisdn: String,
            bytes: Long)

    fun releaseReservedDataBucketEvent(
            msisdn: String,
            bytes: Long)

    // FixMe : For now we assume that there is only 1 MSCC in the Request.
    fun injectCreditControlRequestIntoRingbuffer(
            request: CreditControlRequestInfo,
            streamId: String)
}