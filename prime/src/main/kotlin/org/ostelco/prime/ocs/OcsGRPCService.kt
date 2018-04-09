package org.ostelco.prime.ocs

import io.grpc.stub.StreamObserver
import org.ostelco.ocs.api.ActivateRequest
import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.OcsServiceGrpc
import org.ostelco.prime.logger
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
class OcsGRPCService(private val ocsService: OcsService) : OcsServiceGrpc.OcsServiceImplBase() {

    private val LOG by logger()

    /**
     * Method to handle Credit-Control-Requests
     *
     * @param creditControlAnswer Stream used to send Credit-Control-Answer back to requester
     */
    override fun creditControlRequest(
            creditControlAnswer: StreamObserver<CreditControlAnswerInfo>): StreamObserver<CreditControlRequestInfo> {

        val streamId = newUniqueStreamId()

        LOG.info("Starting Credit-Control-Request with streamId: {}", streamId)

        ocsService.putCreditControlClient(streamId, creditControlAnswer)

        return StreamObserverForStreamWithId(streamId)
    }

    /**
     * Return an unique ID based on Java's UUID generator that uniquely
     * identifies a stream of values.
     * @return A new unique identifier.
     */
    private fun newUniqueStreamId(): String {
        return UUID.randomUUID().toString()
    }

    private inner class StreamObserverForStreamWithId internal constructor(private val streamId: String) : StreamObserver<CreditControlRequestInfo> {

        /**
         * This method gets called every time a Credit-Control-Request is received
         * from the OCS.
         * @param request
         */
        override fun onNext(request: CreditControlRequestInfo) {
            LOG.info("Received Credit-Control-Request request :: " + "for MSISDN: {} with request id: {}",
                    request.msisdn, request.requestId)

            ocsService.creditControlRequestEvent(request, streamId)
        }

        override fun onError(t: Throwable) {
            // TODO vihang: this is important?
        }

        override fun onCompleted() {
            LOG.info("Credit-Control-Request with streamId: {} completed", streamId)
            ocsService.deleteCreditControlClient(streamId)
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

        // The session we have with the OCS will only have one
        // activation invocation. Thus it makes sense to keep the
        // return channel (the activateResponse instance) in a
        // particular place, so that's what we do.  The reason this
        // code looks brittle is that if we ever get multiple activate
        // requests, it will break.   The reason it never breaks
        // is that we never do get more than one.  So yes, it's brittle.
        ocsService.updateActivateResponse(activateResponse)

        val response = ActivateResponse.newBuilder().setMsisdn("").build()

        activateResponse.onNext(response)
    }
}
