package com.telenordigital.prime.ocs;

import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public final class OcsServerImplBaseImpl extends OcsServiceGrpc.OcsServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(OcsServerImplBaseImpl.class);

    private final  OcsService ocsService;

    /**
     * Method to fetch data bucket.
     *
     * @param fetchDataBucketResponse
     */
    @Override
    public StreamObserver<FetchDataBucketInfo> fetchDataBucket(
            final StreamObserver<FetchDataBucketInfo> fetchDataBucketResponse) {

        final String streamId = RandomStringUtils.randomAlphanumeric(22);

        LOG.info("Starting fetchDataBucket with streamId: {}", streamId);

        ocsService.putDataBucketClient(streamId, fetchDataBucketResponse);

        return new StreamObserverForStreamWithId(streamId);
    }

    public OcsServerImplBaseImpl(final OcsService ocsService) {
        this.ocsService = checkNotNull(ocsService);
    }


    private final class StreamObserverForStreamWithId
            implements StreamObserver<FetchDataBucketInfo> {
        private final String streamId;

        StreamObserverForStreamWithId(final String streamId) {
            this.streamId = checkNotNull(streamId);
        }

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

        final String streamId = RandomStringUtils.randomAlphanumeric(22);

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

        @Override
        public void onCompleted() {
            LOG.info("returnUnusedData with streamId: {} completed", streamId);
            ocsService.removeUnusedDataClient(streamId);
        }
    }

    /**
     * Method to receive stream of Activate events.
     *
     * @param request
     * @param activateResponse
     */
    @Override
    public void activate(
            final ActivateRequest request,
            final StreamObserver<ActivateResponse> activateResponse) {

        // The `ActivateRequest` does not have any fields, and so it is ignored.
        // In return, the server starts to send "stream" of `ActivateResponse`
        // which is actually a "request".
        ocsService.updateActivateResponse(activateResponse);

        // After the connection, the first response will have empty string as MSISDN.
        // It should to be ignored by OCS gateway.
        final ActivateResponse response = ActivateResponse.newBuilder().
                setMsisdn("").
                build();

        activateResponse.onNext(response);
    }
}
