package com.telenordigital.prime.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.telenordigital.prime.ocs.FetchDataBucketInfo;

import static com.telenordigital.prime.disruptor.PrimeEventMessageType.FETCH_DATA_BUCKET;
import static com.telenordigital.prime.disruptor.PrimeEventMessageType.RETURN_UNUSED_DATA_BUCKET;
import static com.telenordigital.prime.disruptor.PrimeEventMessageType.TOPUP_DATA_BUNDLE_BALANCE;

/**
 * @author Vihang Patil <vihang.patil@telenordigital.com>
 */
public class PrimeEventProducer {

    private final RingBuffer<PrimeEvent> ringBuffer;

    public PrimeEventProducer(final RingBuffer<PrimeEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void topupDataBundleBalanceEvent(
            final String msisdn,
            final long bytes) {

        long sequence = ringBuffer.next();
        try {
            final PrimeEvent event = ringBuffer.get(sequence);
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

        long sequence = ringBuffer.next();
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

        long sequence = ringBuffer.next();
        try {
            final PrimeEvent event = ringBuffer.get(sequence);
            event.setMessageType(FETCH_DATA_BUCKET);
            event.setMsisdn(request.getMsisdn());
            event.setBucketBytes(request.getBytes());
            event.setOcsgwStreamId(streamId);
            event.setOcsgwRequestId(request.getRequestId());
        } finally {
            ringBuffer.publish(sequence);
        }

    }

}
