package com.telenordigital.prime.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.telenordigital.prime.ocs.CreditControlRequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.telenordigital.prime.disruptor.PrimeEventMessageType.CREDIT_CONTROL_REQUEST;
import static com.telenordigital.prime.disruptor.PrimeEventMessageType.RELEASE_RESERVED_BUCKET;
import static com.telenordigital.prime.disruptor.PrimeEventMessageType.TOPUP_DATA_BUNDLE_BALANCE;

public final class PrimeEventProducer {

    private static final Logger LOG = LoggerFactory.getLogger(PrimeEventProducer.class);

    private final RingBuffer<PrimeEvent> ringBuffer;

    public PrimeEventProducer(final RingBuffer<PrimeEvent> ringBuffer) {
        this.ringBuffer = checkNotNull(ringBuffer);
    }

    private void processNextEventOnTheRingbuffer(final Consumer<PrimeEvent> consumer) {
        checkNotNull(consumer);

        // pick
        final long sequence = ringBuffer.next();
        try {
            final PrimeEvent event = ringBuffer.get(sequence);

            // XXX If event == null, then we're a bit screwed.
            if (event == null) {
                // XXX Actually this is wrong, we're ignoring the update
                //     request since we couldn't get something out of the buffer
                //     altogether different and potentially a lost topup.
                LOG.error("Dropping PrimeEvent update "
                        + "since we couldn't get one off the ringbuffer");
                return;
            }

            // Modify
            consumer.accept(event);

            // Publish
        } finally {  // XXX Why is the "finally" necessary here?
            ringBuffer.publish(sequence);
        }
    }

    private void injectIntoRingbuffer(
            final PrimeEventMessageType type,
            final String msisdn,
            final long requestedBytes,
            final long usedBytes,
            final long reservedBytes,
            final String streamId,
            final String requestId) {
        processNextEventOnTheRingbuffer(event ->
                event.update(type,
                        msisdn,
                        requestedBytes,
                        usedBytes,
                        reservedBytes,
                        streamId,
                        requestId));
    }

    public void topupDataBundleBalanceEvent(
            final String msisdn,
            final long bytes) {
        injectIntoRingbuffer(TOPUP_DATA_BUNDLE_BALANCE,
                msisdn,
                bytes,
                0,
                0,
                null,
                null);
    }

    public void releaseReservedDataBucketEvent(
            final String msisdn,
            final long bytes) {
        injectIntoRingbuffer(RELEASE_RESERVED_BUCKET,
                msisdn,
                0,
                0,
                bytes,
                null,
                null
        );
    }

    // FixMe : For now we assume that there is only 1 MSCC in the Request.
    public void injectCreditControlRequestIntoRingbuffer(
            final CreditControlRequestInfo request,
            final String streamId) {
        injectIntoRingbuffer(CREDIT_CONTROL_REQUEST,
                request.getMsisdn(),
                request.getMscc(0).getRequested().getTotalOctets(),
                request.getMscc(0).getUsed().getTotalOctets(),
                0,
                streamId,
                request.getRequestId());
    }
}
