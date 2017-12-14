package com.telenordigital.prime.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.telenordigital.prime.ocs.FetchDataBucketInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                LOG.info("Ignoring null event");
                return;
            }
            event.setMessageType(TOPUP_DATA_BUNDLE_BALANCE);
            event.setMsisdn(msisdn);
            event.setBucketBytes(bytes);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public void returnUnusedDataBucketEvent(
            final String msisdn,
            final long bytes,
            final String ocsgwStreamId) {

        final long sequence = ringBuffer.next();
        try {
            final PrimeEvent event = ringBuffer.get(sequence);
            event.setMessageType(RETURN_UNUSED_DATA_BUCKET);
            event.setMsisdn(msisdn);
            event.setBucketBytes(bytes);
            event.setOcsgwStreamId(ocsgwStreamId);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public void fetchDataBucketEvent(
            final FetchDataBucketInfo request,
            final String streamId) {

        final long sequence = ringBuffer.next();
        final PrimeEvent event = ringBuffer.get(sequence);
        setEventFields(request, streamId, event);
        ringBuffer.publish(sequence);
    }

    private void setEventFields(
            final FetchDataBucketInfo request,
            final String streamId,
            final PrimeEvent event) {
        event.setMessageType(FETCH_DATA_BUCKET);
        event.setMsisdn(request.getMsisdn());
        event.setBucketBytes(request.getBytes());
        event.setOcsgwStreamId(streamId);
        event.setOcsgwRequestId(request.getRequestId());
    }
}
