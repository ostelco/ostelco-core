package org.ostelco.prime.ocs;

import io.grpc.stub.StreamObserver;
import org.ostelco.ocs.api.ActivateRequest;
import org.ostelco.ocs.api.ActivateResponse;
import org.ostelco.ocs.api.CreditControlAnswerInfo;
import org.ostelco.ocs.api.CreditControlRequestInfo;
import org.ostelco.ocs.api.OcsServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * A service that can be used to serve incoming GRPC requests.
 * is typically bound to a service port using the GRPC ServerBuilder mechanism
 * provide by GRPC:
 *
 * <code>
 *     server = ServerBuilder.
 *         forPort(port).
 *         addService(service).
 *         build();
 * </code>
 *
 * It's implemented as a subclass of {@link  OcsServiceGrpc.OcsServiceImplBase } overriding
 * methods that together implements the protocol described in the  ocs.proto file:
 *
 * <code>
 *     // OCS Service
 *     service OcsService {
 *     rpc CreditControlRequest (stream CreditControlRequestInfo) returns (stream CreditControlAnswerInfo) {}
 *     rpc Activate (ActivateRequest) returns (stream ActivateResponse) {}
 *     }
 * </code>
 *
 * The "stream" type parameters represents sequences of responses, so typically we will here
 * see that a client invokes a method, and listens for a stream of information related to
 * that particular stream.
 */
public final class OcsGRPCService extends OcsServiceGrpc.OcsServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(OcsGRPCService.class);

    private final  OcsService ocsService;

    public OcsGRPCService(final OcsService ocsService) {
        this.ocsService = checkNotNull(ocsService);
    }

    /**
     * Method to handle Credit-Control-Requests
     *
     * @param creditControlAnswer Stream used to send Credit-Control-Answer back to requester
     */
    @Override
    public StreamObserver<CreditControlRequestInfo> creditControlRequest(
            final StreamObserver<CreditControlAnswerInfo> creditControlAnswer) {

        final String streamId = newUniqueStreamId();

        LOG.info("Starting Credit-Control-Request with streamId: {}", streamId);

        ocsService.putCreditControlClient(streamId, creditControlAnswer);

        return new StreamObserverForStreamWithId(streamId);
    }

    /**
     * Return an unique ID based on Java's UUID generator that uniquely
     * identifies a stream of values.
     * @return A new unique identifier.
     */
    private String newUniqueStreamId() {
        return UUID.randomUUID().toString();
    }

    private final class StreamObserverForStreamWithId
            implements StreamObserver<CreditControlRequestInfo> {
        private final String streamId;

        StreamObserverForStreamWithId(final String streamId) {
            this.streamId = checkNotNull(streamId);
        }

        /**
         * This method gets called every time a Credit-Control-Request is received
         * from the OCS.
         * @param request
         */
        @Override
        public void onNext(final CreditControlRequestInfo request) {
            LOG.info("Received Credit-Control-Request request :: "
                            + "for MSISDN: {} with request id: {}",
                    request.getMsisdn(), request.getRequestId());

            ocsService.creditControlRequestEvent(request, streamId);
        }

        @Override
        public void onError(final Throwable t) {
            // TODO vihang: this is important?
        }

        @Override
        public void onCompleted() {
            LOG.info("Credit-Control-Request with streamId: {} completed", streamId);
            ocsService.deleteCreditControlClient(streamId);
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
    @Override
    public void activate(
            final ActivateRequest request,
            final StreamObserver<ActivateResponse> activateResponse) {

        // The session we have with the OCS will only have one
        // activation invocation. Thus it makes sense to keep the
        // return channel (the activateResponse instance) in a
        // particular place, so that's what we do.  The reason this
        // code looks brittle is that if we ever get multiple activate
        // requests, it will break.   The reason it never breaks
        // is that we never do get more than one.  So yes, it's brittle.
        ocsService.updateActivateResponse(activateResponse);

        final ActivateResponse response = ActivateResponse.newBuilder().
                setMsisdn("").
                build();

        activateResponse.onNext(response);
    }
}
