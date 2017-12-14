package com.telenordigital.prime.ocs;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * A service that can be used to serve incoming GRPC requests.   The service
 * is typically bound to a service port using the {@link ServerBuilder} mechanism
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
 * three  methods that together implements the protocol described in the  ocs.proto file:
 *
 * <code>
 *     // OCS Service
 *     service OcsService {
 *     rpc FetchDataBucket (stream FetchDataBucketInfo) returns (stream FetchDataBucketInfo) {}
 *     rpc ReturnUnusedData (stream ReturnUnusedDataRequest) returns (stream ReturnUnusedDataResponse) {}
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
     * Method to fetch data bucket.
     *
     * @param fetchDataBucketResponse
     */
    @Override
    public StreamObserver<FetchDataBucketInfo> fetchDataBucket(
            final StreamObserver<FetchDataBucketInfo> fetchDataBucketResponse) {

        final String streamId = newUniqueStreamId();

        LOG.info("Starting fetchDataBucket with streamId: {}", streamId);

        ocsService.putDataBucketClient(streamId, fetchDataBucketResponse);

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
            implements StreamObserver<FetchDataBucketInfo> {
        private final String streamId;

        StreamObserverForStreamWithId(final String streamId) {
            this.streamId = checkNotNull(streamId);
        }

        /**
         * This method gets called every time a bucket info is requested
         * from the OCS.
         * @param request
         */
        @Override
        public void onNext(final FetchDataBucketInfo request) {
            LOG.info("Received fetchDataBucket request :: "
                            + "for MSISDN: {} of {} bytes with request id: {}",
                    request.getMsisdn(), request.getBytes(), request.getRequestId());

            ocsService.fetchDataBucketEvent(request, streamId);
        }

        @Override
        public void onError(final Throwable t) {
            // TODO vihang: this is important?
        }

        @Override
        public void onCompleted() {
            LOG.info("fetchDataBucket with streamId: {} completed", streamId);
            ocsService.deleteDataBucketClient(streamId);
        }
    }

    /**
     * Method to return Unused Data.
     *
     * @param returnUnusedDataResponse
     */
    @Override
    public StreamObserver<ReturnUnusedDataRequest> returnUnusedData(
            final StreamObserver<ReturnUnusedDataResponse> returnUnusedDataResponse) {

        final String streamId = newUniqueStreamId();

        LOG.info("Starting returnUnusedData with streamId: {}", streamId);

        ocsService.registerUnusedDataClient(streamId, returnUnusedDataResponse);
        return new StreamObserverForReturnedUnusedData(streamId);
    }

    private final class StreamObserverForReturnedUnusedData
            implements StreamObserver<ReturnUnusedDataRequest> {

        private final String streamId;

        StreamObserverForReturnedUnusedData(final String streamId) {
            this.streamId = streamId;
        }

        /**
         * Invoked when receiving another request for info on
         * unused data.  Sent up to the OCS  service.
         * @param request  An incoming request decoded from the wire protocol.
         */
        @Override
        public void onNext(final ReturnUnusedDataRequest request) {
            LOG.info("Received returnUnusedData request :: for MSISDN: {} of {} bytes",
                    request.getMsisdn(), request.getBytes());
            ocsService.returnUnusedDataBucketEvent(
                    request.getMsisdn(),
                    request.getBytes(),
                    streamId);
        }

        @Override
        public void onError(final Throwable t) {
            LOG.warn("Exception for returnUnusedData", t);
        }

        /**
         * Orderly shutdown of a possibly long interaction of requesting
         * and delivering information.
         */
        @Override
        public void onCompleted() {
            LOG.info("returnUnusedData with streamId: {} completed", streamId);
            ocsService.removeUnusedDataClient(streamId);
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

        ocsService.updateActivateResponse(activateResponse);

        final ActivateResponse response = ActivateResponse.newBuilder().
                setMsisdn("").
                build();

        activateResponse.onNext(response);
    }
}
