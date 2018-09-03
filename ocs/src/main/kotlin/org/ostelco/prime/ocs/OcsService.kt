package org.ostelco.prime.ocs

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

class OcsService(private val producer: EventProducer) {

    private val creditControlClientMap: ConcurrentMap<String, StreamObserver<CreditControlAnswerInfo>>

    /**
     * A holder for
     * [<]
     * instances that are somehow used
     */
    private val activateResponseHolder: ActivateResponseHolder

    private val eventHandler: EventHandler<OcsEvent>

    private val ocsServerImplBaseImpl: OcsServiceGrpc.OcsServiceImplBase

    init {
        this.creditControlClientMap = ConcurrentHashMap()
        this.eventHandler = EventHandlerImpl(this)
        this.ocsServerImplBaseImpl = OcsGrpcService(this)
        this.activateResponseHolder = ActivateResponseHolder()
    }

    fun asEventHandler(): EventHandler<OcsEvent> {
        return eventHandler
    }

    fun returnUnusedDataBucketEvent(
            msisdn: String,
            bucketBytes: Long) {
        producer.releaseReservedDataBucketEvent(
                msisdn,
                bucketBytes)
    }

    /**
     * Return a service that can be used to serve incoming GRPC requests.   The service
     * is typically bound to a service port using the GRPC ServerBuilder mechanism
     * provide by GRPC:
     * `
     * server = ServerBuilder.
     * forPort(port).
     * addService(service).
     * build();
    ` *
     *
     * @return The service that can receive incoming GRPC messages
     */
    fun asOcsServiceImplBase(): OcsServiceGrpc.OcsServiceImplBase {
        return this.ocsServerImplBaseImpl
    }

    private fun getCreditControlClientForStream(
            streamId: String): StreamObserver<CreditControlAnswerInfo>? {
        // Here we need to Convert it back to an answer.
        creditControlClientMap[streamId]
        return creditControlClientMap[streamId]
    }

    fun activateOnNextResponse(response: ActivateResponse) {
        this.activateResponseHolder.onNextResponse(response)
    }

    fun updateActivateResponse(
            activateResponse: StreamObserver<ActivateResponse>) {
        this.activateResponseHolder.setActivateResponse(activateResponse)
    }

    fun deleteCreditControlClient(streamId: String) {
        this.creditControlClientMap.remove(streamId)
    }

    fun creditControlRequestEvent(
            request: CreditControlRequestInfo,
            streamId: String) {
        producer.injectCreditControlRequestIntoRingbuffer(request, streamId)
    }

    fun putCreditControlClient(
            streamId: String,
            creditControlAnswer: StreamObserver<CreditControlAnswerInfo>) {
        creditControlClientMap[streamId] = creditControlAnswer
    }

    fun sendCreditControlAnswer(streamId: String, creditControlAnswerInfo: CreditControlAnswerInfo) {
        val creditControlAnswer = getCreditControlClientForStream(streamId)

        creditControlAnswer?.onNext(creditControlAnswerInfo)
    }
}
