package org.ostelco.prime.ocs.consumption

import io.grpc.stub.StreamObserver
import org.ostelco.ocs.api.ActivateRequest
import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.CreditControlRequestType.NONE
import org.ostelco.ocs.api.OcsServiceGrpc
import org.ostelco.prime.getLogger
import java.util.*

/**
 * A service that can be used to serve incoming GRPC requests.
 * is typically bound to a service port using the GRPC ServerBuilder mechanism
 * provide by GRPC:
 *
 * `
 * server = ServerBuilder.
 * forPort(port).
 * addService(service).
 * build();
` *
 *
 * It's implemented as a subclass of [OcsServiceGrpc.OcsServiceImplBase] overriding
 * methods that together implements the protocol described in the  ocs.proto file:
 *
 * `
 * // OCS Service
 * service OcsService {
 * rpc CreditControlRequest (stream CreditControlRequestInfo) returns (stream CreditControlAnswerInfo) {}
 * rpc Activate (ActivateRequest) returns (stream ActivateResponse) {}
 * }
` *
 *
 * The "stream" type parameters represents sequences of responses, so typically we will here
 * see that a client invokes a method, and listens for a stream of information related to
 * that particular stream.
 */
class OcsGrpcService(private val ocsAsyncRequestConsumer: OcsAsyncRequestConsumer) : OcsServiceGrpc.OcsServiceImplBase() {

    private val logger by getLogger()

    /**
     * Method to handle Credit-Control-Requests
     *
     * @param creditControlAnswer Stream used to send Credit-Control-Answer back to requester
     */
    override fun creditControlRequest(creditControlAnswer: StreamObserver<CreditControlAnswerInfo>): StreamObserver<CreditControlRequestInfo> {

        val streamId = newUniqueStreamId()
        logger.info("Starting Credit-Control-Request with streamId: {}", streamId)
        ocsAsyncRequestConsumer.putCreditControlClient(streamId, creditControlAnswer)
        return StreamObserverForStreamWithId(streamId)
    }

    private inner class StreamObserverForStreamWithId(private val streamId: String) : StreamObserver<CreditControlRequestInfo> {

        /**
         * This method gets called every time a Credit-Control-Request is received
         * from the OCS.
         * @param request
         */
        override fun onNext(request: CreditControlRequestInfo) {
            if(request.type == NONE) {
                // this request is just to keep connection alive
                return
            }
            logger.info("Received Credit-Control-Request request :: " + "for MSISDN: {} with request id: {}",
                    request.msisdn, request.requestId)

            ocsAsyncRequestConsumer.creditControlRequestEvent(streamId = streamId, request = request)
        }

        override fun onError(t: Throwable) {
            // TODO vihang: handle onError for stream observers
        }

        override fun onCompleted() {
            logger.info("Credit-Control-Request with streamId: {} completed", streamId)
            ocsAsyncRequestConsumer.deleteCreditControlClient(streamId)
        }
    }

    /**
     * The `ActivateRequest` does not have any fields, and so it is ignored.
     * In return, the server starts to send "stream" of `ActivateResponse`
     * which is actually a "request".
     *
     * After the connection, the first response will have empty string as MSISDN.
     * It should to be ignored by OCS gateway.   This method sends that empty
     * response back to the invoker.
     *
     * @param request  Is ignored.
     * @param activateResponse the stream observer used to send the response back.
     */
    override fun activate(
            request: ActivateRequest,
            activateResponse: StreamObserver<ActivateResponse>) {

        val streamId = newUniqueStreamId()
        logger.info("Starting Activate-Response stream with streamId: {}", streamId)
        ocsAsyncRequestConsumer.updateActivateResponse(streamId, activateResponse)

        val initialDummyResponse = ActivateResponse.newBuilder().setMsisdn("").build()
        activateResponse.onNext(initialDummyResponse)
    }

    private fun newUniqueStreamId(): String {
        return UUID.randomUUID().toString()
    }
}
