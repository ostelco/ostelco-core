package com.telenordigital.prime.disruptor;

import lombok.Data;

/**
 * @author Vihang Patil (vihang.patil@telenordigital.com)
 */
@Data
public final class PrimeEvent {

    /**
     * Phone number this event is related to.
     */
    private String msisdn;

    /**
     *  Origin of word 'bucket' - PGw consumes data in `buckets` of 10 MB ~ 100 MB at a time
     *  This field is used in...
     *  1. Request to fetch bucket
     *  2. Response of actual allowed bucket
     *  3. Request to return unused bucket
     */
    private long bucketBytes;

    /**
     *  Origin of word 'bundle' - Subscriber buys a 'bundle' of units of data/airtime/validity etc.
     *  This field represent total balance bytes.
     */
    private long bundleBytes;

    /**
     *  Stream ID used to correlate response with correct gRPC stream.
     */
    private String ocsgwStreamId;

    /**
     *  Request ID used by OCS gateway to correlate response with requests
     */
    private String ocsgwRequestId;

    /**
     * The type of message this is, closely mirroring the types of messages in
     * ocs.proto (the GRPC specification file).
     */
    private PrimeEventMessageType messageType;

    public void clear() {
        msisdn = null;
        bucketBytes = 0;
        bundleBytes = 0;
        ocsgwStreamId = null;
        ocsgwRequestId = null;
        messageType = null;
    }

    public void update(
            final PrimeEventMessageType messageType,
            final String msisdn,
            final long bytes,
            final String ocsgwStreamId,
            final String ocsgwRequestId) {
        this.setMessageType(messageType);
        this.setMsisdn(msisdn);
        this.setBucketBytes(bytes);
        this.setOcsgwStreamId(ocsgwStreamId);
        this.setOcsgwRequestId(ocsgwRequestId);
    }
}
