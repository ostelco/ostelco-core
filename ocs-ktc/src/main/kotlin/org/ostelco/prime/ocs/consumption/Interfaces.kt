package org.ostelco.prime.ocs.consumption

import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo

/**
 * Ocs Requests from [OcsGrpcService] are consumed by implementation [OcsService] of [OcsAsyncRequestConsumer]
 */
interface OcsAsyncRequestConsumer {
    fun creditControlRequestEvent(
            request: CreditControlRequestInfo,
            returnCreditControlAnswer:
            (CreditControlAnswerInfo) -> Unit)
}

/**
 * Ocs Events from [OcsEventToGrpcResponseMapper] forwarded to implementation [OcsService] of [OcsAsyncResponseProducer]
 */
interface OcsAsyncResponseProducer {
    fun activateOnNextResponse(response: ActivateResponse)
    fun sendCreditControlAnswer(streamId: String, creditControlAnswer: CreditControlAnswerInfo)
    fun returnUnusedDataBucketEvent(msisdn: String, reservedBucketBytes: Long)
}