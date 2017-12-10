package com.telenordigital.prime.disruptor;

/**
 * @author Vihang Patil <vihang.patil@telenordigital.com>
 */
public final class PrimeEvent {

    private String msisdn;

    // Origin of word 'bucket' - PGw consumes data in `buckets` of 10 MB ~ 100 MB at a time.
    // This field is used in...
    // 1. Request to fetch bucket
    // 2. Response of actual allowed bucket
    // 3. Request to return unused bucket
    private long bucketBytes;

    // Origin of word 'bundle' - Subscriber buys a 'bundle' of units of data/airtime/validity etc.
    // This field represent total balance bytes.
    private long bundleBytes;

    // Stream ID used to correlate response with correct gRPC stream
    private String ocsgwStreamId;

    // Request ID used by OCS gateway to correlate response with requests
    private String ocsgwRequestId;

    private PrimeEventMessageType messageType;

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(final String msisdn) {
        this.msisdn = msisdn;
    }

    public long getBucketBytes() {
        return bucketBytes;
    }

    public void setBucketBytes(final long bucketBytes) {
        this.bucketBytes = bucketBytes;
    }

    public long getBundleBytes() {
        return bundleBytes;
    }

    public void setBundleBytes(final long bundleBytes) {
        this.bundleBytes = bundleBytes;
    }

    public String getOcsgwStreamId() {
        return ocsgwStreamId;
    }

    public void setOcsgwStreamId(final String ocsgwStreamId) {
        this.ocsgwStreamId = ocsgwStreamId;
    }

    public String getOcsgwRequestId() {
        return ocsgwRequestId;
    }

    public void setOcsgwRequestId(final String ocsgwRequestId) {
        this.ocsgwRequestId = ocsgwRequestId;
    }

    public PrimeEventMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(final PrimeEventMessageType messageType) {
        this.messageType = messageType;
    }

    public void clear() {
        msisdn = null;
        bucketBytes = 0;
        bundleBytes = 0;
        ocsgwStreamId = null;
        ocsgwRequestId = null;
        messageType = null;
    }
}
