package com.telenordigital.prime.ocs;

import com.lmax.disruptor.EventHandler;
import com.telenordigital.prime.disruptor.PrimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An event handler, handling the {@link PrimeEvent} messages that
 * are used by the Disruptor execution mechanism to handle events.
 */
final class EventHandlerImpl implements EventHandler<PrimeEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(EventHandlerImpl.class);

    private final OcsService ocsService;

    EventHandlerImpl(final OcsService ocsService) {
        this.ocsService = checkNotNull(ocsService);
    }

    @Override
    public void onEvent(
            final PrimeEvent event,
            final long sequence,
            final boolean endOfBatch) {

        try {
            dispatchOnEventType(event);
        } catch (Exception e) {
            LOG.warn("Exception handling prime event in OcsService", e);
            // XXX Should the exception be cast further up the call chain?
        }
    }

    private void dispatchOnEventType(final PrimeEvent event) {
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
                LOG.warn("Unknown event type " + event.getMessageType());
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
        if (event.getOcsgwStreamId() == null) {
            LOG.error("Null event, dropping it");
            return;
        }

        LOG.info("Returning returnUnusedData response :: for MSISDN: {}", event.getMsisdn());

        final ReturnUnusedDataResponse returnDataInfo =
                ReturnUnusedDataResponse.newBuilder().
                        setMsisdn(event.getMsisdn()).
                        build();
        ocsService.replyWithReturnDataInfo(event.getOcsgwStreamId(), returnDataInfo);
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
            ocsService.replyWithDataBucketInfo(event.getOcsgwStreamId(), fetchDataInfo);
        } catch (Exception e) {
            LOG.warn("Exception handling prime event", e);
            logEventPRocessing("Exception returning fetchDataBucket response", event);

            // unable to send fetchDataBucket response.
            // So, return bucket bytes back to data bundle.
            ocsService.returnUnusedDataBucketEvent(
                    event.getMsisdn(),
                    event.getBucketBytes());
        }
    }
}
