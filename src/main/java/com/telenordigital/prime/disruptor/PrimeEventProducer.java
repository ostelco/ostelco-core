package com.telenordigital.prime.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.telenordigital.prime.ocs.FetchDataBucketInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.telenordigital.prime.disruptor.PrimeEventMessageType.FETCH_DATA_BUCKET;
import static com.telenordigital.prime.disruptor.PrimeEventMessageType.RETURN_UNUSED_DATA_BUCKET;
import static com.telenordigital.prime.disruptor.PrimeEventMessageType.TOPUP_DATA_BUNDLE_BALANCE;

/**
 * @author Vihang Patil <vihang.patil@telenordigital.com>
 */
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
                // XXX Actually this is wrong, we're ignoring the topup
                //     request since we couldn't get something out of the buffer
                //     altogether different and potentially a lost topup.
                LOG.info("Ignoring null event");
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
            final long bytes,
            final String streamId,
            final String requestId) {
        processNextEventOnTheRingbuffer(event ->
                event.update(type,
                        msisdn,
                        bytes,
                        streamId,
                        requestId));
    }

    public void topupDataBundleBalanceEvent(
            final String msisdn,
            final long bytes) {
        injectIntoRingbuffer(TOPUP_DATA_BUNDLE_BALANCE,
                msisdn,
                bytes,
                null,
                null);
    }

    public void returnUnusedDataBucketEvent(
            final String msisdn,
            final long bytes,
            final String ocsgwStreamId) {
        injectIntoRingbuffer(RETURN_UNUSED_DATA_BUCKET,
                msisdn,
                bytes,
                ocsgwStreamId,
                null);
    }

    public void injectFetchDataBucketRequestIntoRingbuffer(
            final FetchDataBucketInfo request,
            final String streamId) {
        injectIntoRingbuffer(FETCH_DATA_BUCKET,
                request.getMsisdn(),
                request.getBytes(),
                streamId,
                request.getRequestId());
    }
}
