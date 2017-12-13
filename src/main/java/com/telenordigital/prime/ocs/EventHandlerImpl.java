package com.telenordigital.prime.ocs;

import com.lmax.disruptor.EventHandler;
import com.telenordigital.prime.disruptor.PrimeEvent;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

final class EventHandlerImpl implements EventHandler<PrimeEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(EventHandlerImpl.class);

    private final  OcsService ocsService;

    public EventHandlerImpl(final OcsService ocsService) {
        this.ocsService = checkNotNull(ocsService);
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
        ocsService.activateOnNextResponse(response);
    }

    private void handleReturnUnusedDataBucket(final PrimeEvent event) {
        if (event.getOcsgwStreamId() != null) {
            LOG.info("Returning returnUnusedData response :: for MSISDN: {}", event.getMsisdn());

            final ReturnUnusedDataResponse returnDataInfo =
                    ReturnUnusedDataResponse.newBuilder().
                            setMsisdn(event.getMsisdn()).
                            build();
            final StreamObserver<ReturnUnusedDataResponse> returnUnusedDataResponse
                   =  ocsService.getUnusedDataClientForStream(event.getOcsgwStreamId());
            if (returnUnusedDataResponse != null) {
                returnUnusedDataResponse.onNext(returnDataInfo);
            }
        }
    }

    private void logEventPRocessing(final String msg, final PrimeEvent event) {
        LOG.info("{} :: for MSISDN: {} of {} bytes with request id: {}",
                msg, event.getMsisdn(), event.getBucketBytes(), event.getOcsgwRequestId());
    }

    private void handleFetchDataBucket(final PrimeEvent event) {

        logEventPRocessing("Returning fetchDataBucket response", event);

        try {
            final FetchDataBucketInfo fetchDataInfo =
                    FetchDataBucketInfo.newBuilder().
                            setMsisdn(event.getMsisdn()).
                            setBytes(event.getBucketBytes()).
                            setRequestId(event.getOcsgwRequestId()).
                            build();

            final StreamObserver<FetchDataBucketInfo> fetchDataBucketResponse
            = ocsService.getDataBucketClientForStream(event.getOcsgwStreamId());

            if (fetchDataBucketResponse != null) {
                fetchDataBucketResponse.onNext(fetchDataInfo);
            }
        } catch (Exception e) {
            LOG.warn("Exception handling prime event", e);
            logEventPRocessing("Exception returning fetchDataBucket response", event);
            // unable to send fetchDataBucket response. So, return bucket bytes back to data bundle.
            ocsService.returnUnusedDataBucketEvent(
                    event.getMsisdn(),
                    event.getBucketBytes());
        }
    }
}
