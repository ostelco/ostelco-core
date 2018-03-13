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
            case CREDIT_CONTROL_REQUEST:
                handleCreditControlRequest(event);
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

    private void logEventPRocessing(final String msg, final PrimeEvent event) {
        LOG.info("{} :: for MSISDN: {} of {} requested bytes {} used bytes with request id: {}",
                msg, event.getMsisdn(), event.getRequestedBucketBytes(), event.getUsedBucketBytes() ,event.getOcsgwRequestId());
    }

    private void handleCreditControlRequest(final PrimeEvent event) {

        logEventPRocessing("Returning Credit-Control-Answer", event);

        // FixMe: This assume we only have one MSCC
        try {
            final CreditControlAnswerInfo creditControlAnswer =
                    CreditControlAnswerInfo.newBuilder()
                            .setMsisdn(event.getMsisdn())
                            .addMscc(MultipleServiceCreditControl.newBuilder()
                                    .setGranted(ServiceUnit.newBuilder()
                                            .setTotalOctets(event.getReservedBucketBytes())
                                            .build())
                                    .setServiceIdentifier(event.getServiceIdentifier())
                                    .setRatingGroup(event.getRatingGroup())
                                    .setValidityTime(86400)
                                    .build())
                            .setRequestId(event.getOcsgwRequestId())
                            .build();
            ocsService.sendCreditControlAnswer(event.getOcsgwStreamId(), creditControlAnswer);
        } catch (Exception e) {
            LOG.warn("Exception handling prime event", e);
            logEventPRocessing("Exception sending Credit-Control-Answer", event);

            // unable to send Credit-Control-Answer.
            // So, return reserved bucket bytes back to data bundle.
            ocsService.returnUnusedDataBucketEvent(
                    event.getMsisdn(),
                    event.getReservedBucketBytes());
        }
    }
}
