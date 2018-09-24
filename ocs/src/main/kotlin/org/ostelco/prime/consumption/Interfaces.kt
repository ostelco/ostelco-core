package org.ostelco.prime.consumption

import io.grpc.stub.StreamObserver
import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo

/**
 * Ocs Requests from [OcsGrpcService] are consumed by implementation [OcsService] of [OcsAsyncRequestConsumer]
 */
interface OcsAsyncRequestConsumer {
    fun putCreditControlClient(streamId: String, creditControlAnswer: StreamObserver<CreditControlAnswerInfo>)
    fun creditControlRequestEvent(streamId: String, request: CreditControlRequestInfo)
    fun deleteCreditControlClient(streamId: String)
    fun updateActivateResponse(streamId: String, activateResponse: StreamObserver<ActivateResponse>)
}

/**
 * Ocs Events from [OcsEventToGrpcResponseMapper] forwarded to implementation [OcsService] of [OcsAsyncResponseProducer]
 */
interface OcsAsyncResponseProducer {
    fun activateOnNextResponse(response: ActivateResponse)
    fun sendCreditControlAnswer(streamId: String, creditControlAnswer: CreditControlAnswerInfo)
    fun returnUnusedDataBucketEvent(msisdn: String, reservedBucketBytes: Long)
}