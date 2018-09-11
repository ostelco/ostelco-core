package org.ostelco.prime.consumption

import com.lmax.disruptor.EventHandler
import io.grpc.stub.StreamObserver
import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.OcsServiceGrpc
import org.ostelco.prime.disruptor.EventProducer
import org.ostelco.prime.disruptor.OcsEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * [OcsService] acts as bridge between [OcsGrpcService] and [OcsDisruptor].
 *
 * Ocs Requests from [OcsGrpcService] are sent to [OcsService] via [OcsAsyncRequestConsumer] interface.
 * These requests are then sent to [OcsDisruptor] via [EventProducer].
 * [EventProducerImpl] is the implementation for [EventProducer] interface.
 *
 * OcsEvents from [OcsDisruptor] are handled by [OcsEventToGrpcResponseMapper].
 * [OcsEventToGrpcResponseMapper] forwards responses to [OcsService] via [OcsAsyncResponseProducer] interface.
 * [OcsService] produces responses and sends them to [OcsGrpcService].
 */
class OcsService(private val producer: EventProducer) : OcsAsyncRequestConsumer, OcsAsyncResponseProducer {

    private val creditControlClientMap: ConcurrentMap<String, StreamObserver<CreditControlAnswerInfo>> = ConcurrentHashMap()
    private val activateMsisdnClientMap: ConcurrentMap<String, StreamObserver<ActivateResponse>> = ConcurrentHashMap()

    /**
     * A holder for
     * [<]
     * instances that are somehow used
     */
    val eventHandler: EventHandler<OcsEvent> = OcsEventToGrpcResponseMapper(this)
    val ocsGrpcService: OcsServiceGrpc.OcsServiceImplBase = OcsGrpcService(this)

    //
    // Request Consumer functions
    //

    override fun updateActivateResponse(
            streamId: String,
            activateResponse: StreamObserver<ActivateResponse>) {
        this.activateMsisdnClientMap[streamId] = activateResponse
    }

    override fun deleteCreditControlClient(streamId: String) {
        this.creditControlClientMap.remove(streamId)
    }

    override fun creditControlRequestEvent(
            streamId: String,
            request: CreditControlRequestInfo) {
        producer.injectCreditControlRequestIntoRingbuffer(streamId, request)
    }

    override fun putCreditControlClient(
            streamId: String,
            creditControlAnswer: StreamObserver<CreditControlAnswerInfo>) {
        creditControlClientMap[streamId] = creditControlAnswer
    }

    //
    // Response Producer functions
    //

    override fun sendCreditControlAnswer(
            streamId: String,
            creditControlAnswer: CreditControlAnswerInfo) {
        creditControlClientMap[streamId]?.onNext(creditControlAnswer)
    }

    override fun activateOnNextResponse(response: ActivateResponse) {
        // TODO martin: send activate MSISDN to selective ocsgw instead of all
        this.activateMsisdnClientMap.forEach { _ , responseStream ->
            responseStream.onNext(response)
        }
    }

    override fun returnUnusedDataBucketEvent(
            msisdn: String,
            reservedBucketBytes: Long) {
        producer.releaseReservedDataBucketEvent(
                msisdn,
                reservedBucketBytes)
    }
}
