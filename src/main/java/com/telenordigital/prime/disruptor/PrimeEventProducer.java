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

    public void topupDataBundleBalanceEvent(
            final String msisdn,
            final long bytes) {

        // Get index of next sequence in Ring Buffer
        final long sequence = ringBuffer.next();
        try {
            final PrimeEvent event = ringBuffer.get(sequence);
            if (event == null) {
                // XXX Actually this is wrong, we're ignoring the topup
                //     request since we couldn't get something out of the buffer
                //     altogether different and potentially a lost topup.
                LOG.info("Ignoring null event");
                return;
            }
            event.update(TOPUP_DATA_BUNDLE_BALANCE, msisdn, bytes, null, null);
        } finally {
            // XXX ??? Why put this in finally, why not just ignore publishing if
            //     anything happens during processing? This probably is a bug, but
            //     I don't know what the proper fix is.
            ringBuffer.publish(sequence);
        }
    }


    private void pickAndPublish(final Consumer<PrimeEvent> consumer) {
        checkNotNull(consumer);

        // pick
        final long sequence = ringBuffer.next();
        try {
            final PrimeEvent event = ringBuffer.get(sequence);

            // Modify
            consumer.accept(event);

            // Publish
        } finally {  // XXX Why is the "finally" necessary here?
            ringBuffer.publish(sequence);
        }
    }

    public void returnUnusedDataBucketEvent(
            final String msisdn,
            final long bytes,
            final String ocsgwStreamId) {
        pickAndPublish(event ->
                event.update(RETURN_UNUSED_DATA_BUCKET,
                        msisdn,
                        bytes,
                        ocsgwStreamId,
                        null));

    }


    public void injectFetchDataBucketRequestIntoRingbuffer(
            final FetchDataBucketInfo request,
            final String streamId) {

        // Get the next event off the ringbuffer
        final long sequence = ringBuffer.next();
        final PrimeEvent event = ringBuffer.get(sequence);

        // Then set all the fields of the event based on
        // info found in the incoming request.

        event.update(FETCH_DATA_BUCKET,
                request.getMsisdn(),
                request.getBytes(),
                streamId,
                request.getRequestId());

        // Finally publish the updated item to the ring buffer.
        ringBuffer.publish(sequence);
    }

}
