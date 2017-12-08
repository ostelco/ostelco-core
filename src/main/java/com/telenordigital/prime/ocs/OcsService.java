package com.telenordigital.prime.ocs;

import com.lmax.disruptor.EventHandler;
import com.telenordigital.prime.disruptor.PrimeEvent;
import com.telenordigital.prime.disruptor.PrimeEventProducer;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Vihang Patil <vihang.patil@telenordigital.com>
 */
public final class OcsService
        extends OcsServiceGrpc.OcsServiceImplBase
        implements EventHandler<PrimeEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OcsService.class);

    private ConcurrentMap<String, StreamObserver<FetchDataBucketInfo>> fetchDataBucketClientMap = new ConcurrentHashMap<>();
    private ConcurrentMap<String, StreamObserver<ReturnUnusedDataResponse>> returnUnusedDataClientMap = new ConcurrentHashMap<>();
    private StreamObserver<ActivateResponse> activateResponse;

    private final PrimeEventProducer producer;

    public OcsService(final PrimeEventProducer producer) {
        this.producer = producer;
    }

    @Override
    public void onEvent(
            final PrimeEvent event,
            final long sequence,
            final boolean endOfBatch) throws Exception {

        try {
            switch (event.getMessageType()) {
                case FETCH_DATA_BUCKET:
                    handleFetchDataBucket(event);
                    break;

                case RETURN_UNUSED_DATA_BUCKET:
                    handleReturnUnusedDataBucket(event);
                    break;

                case TOPUP_DATA_BUNDLE_BALANCE:
                    handleTopupDataBundleBalance(event);
                    break;
                default:
            }
        } catch (Exception e) {
            LOG.warn("Exception handling prime event in OcsService", e);
        }
    }

    private void handleTopupDataBundleBalance(final PrimeEvent event) {
        final ActivateResponse response =
                ActivateResponse.newBuilder().
                        setMsisdn(event.getMsisdn()).
                        build();
        if (activateResponse != null) {
            activateResponse.onNext(response);
        }
    }

    private void handleReturnUnusedDataBucket(final PrimeEvent event) {
        if (event.getOcsgwStreamId() != null) {
            LOG.info("Returning returnUnusedData response :: for MSISDN: {}", event.getMsisdn());

            final ReturnUnusedDataResponse returnDataInfo =
                    ReturnUnusedDataResponse.newBuilder()
                            .setMsisdn(event.getMsisdn())
                            .build();
            final StreamObserver<ReturnUnusedDataResponse> returnUnusedDataResponse
                    = returnUnusedDataClientMap.get(event.getOcsgwStreamId());
            if (returnUnusedDataResponse != null) {
                returnUnusedDataResponse.onNext(returnDataInfo);
            }
        }
    }

    private void handleFetchDataBucket(PrimeEvent event) {
        try {
            LOG.info("Returning fetchDataBucket response :: for MSISDN: {} of {} bytes with request id: {}",
                    event.getMsisdn(), event.getBucketBytes(), event.getOcsgwRequestId());

            final FetchDataBucketInfo fetchDataInfo =
                    FetchDataBucketInfo.newBuilder()
                            .setMsisdn(event.getMsisdn())
                            .setBytes(event.getBucketBytes())
                            .setRequestId(event.getOcsgwRequestId())
                            .build();

            final StreamObserver<FetchDataBucketInfo> fetchDataBucketResponse
                    = fetchDataBucketClientMap.get(event.getOcsgwStreamId());
            if (fetchDataBucketResponse != null) {
                fetchDataBucketResponse.onNext(fetchDataInfo);
            }
        } catch (Exception e) {
            LOG.warn("Exception handling prime event", e);
            LOG.warn("Exception returning fetchDataBucket response :: for MSISDN: {} of {} bytes with request id: {}",
                    event.getMsisdn(), event.getBucketBytes(), event.getOcsgwRequestId());
            // unable to send fetchDataBucket response. So, return bucket bytes back to data bundle.
            producer.returnUnusedDataBucketEvent(event.getMsisdn(), event.getBucketBytes(), null);
        }
    }

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

        fetchDataBucketClientMap.put(streamId, fetchDataBucketResponse);

        return new StreamObserver<FetchDataBucketInfo>() {
            @Override
            public void onNext(final FetchDataBucketInfo request) {
                LOG.info("Received fetchDataBucket request :: for MSISDN: {} of {} bytes with request id: {}",
                        request.getMsisdn(), request.getBytes(), request.getRequestId());

                producer.fetchDataBucketEvent(request, streamId);
            }

            @Override
            public void onError(final Throwable t) {
                // TODO vihang: this is important?
                // LOG.warn("Exception for fetchDataBucket", t);
            }

            @Override
            public void onCompleted() {
                LOG.info("fetchDataBucket with streamId: {} completed", streamId);
                fetchDataBucketClientMap.remove(streamId);
            }
        };
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

        returnUnusedDataClientMap.put(streamId, returnUnusedDataResponse);

        return new StreamObserver<ReturnUnusedDataRequest>() {
            @Override
            public void onNext(final ReturnUnusedDataRequest request) {
                LOG.info("Received returnUnusedData request :: for MSISDN: {} of {} bytes",
                        request.getMsisdn(), request.getBytes());
                producer.returnUnusedDataBucketEvent(
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
                returnUnusedDataClientMap.remove(streamId);
            }
        };
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
        this.activateResponse = activateResponse;

        // After the connection, the first response will have empty string as MSISDN.
        // It should to be ignored by OCS gateway.
        final ActivateResponse response = ActivateResponse.newBuilder().
                setMsisdn("").
                build();

        activateResponse.onNext(response);
    }
}
