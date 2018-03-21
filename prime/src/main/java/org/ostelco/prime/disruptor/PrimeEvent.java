package org.ostelco.prime.disruptor;

import lombok.Data;

@Data
public final class PrimeEvent {

    /**
     * The type of message this is, closely mirroring the types of messages in
     * ocs.proto (the GRPC specification file).
     */
    private PrimeEventMessageType messageType;

    /**
     * Phone number this event is related to.
     */
    private String msisdn;

    /**
     *  Origin of word 'bucket' - P-GW consumes data in `buckets` of 10 MB ~ 100 MB at a time
     *  This field is used in.
     *  Request to reserve a new bucket of bytes
     */
    private long requestedBucketBytes;

    /**
     *  Bytes that has been used from the bucket (previously reserved).
     */
    private long usedBucketBytes;


    /**
     *  Buckets that has been reserved from the bundle.
     */
    private long reservedBucketBytes;

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
     * Service-Identifier is used to classify traffic
     */
    private long serviceIdentifier;

    /**
     * Rating-Group is used to classify traffic
     */
    private long ratingGroup;

    public void clear() {
        msisdn = null;
        requestedBucketBytes = 0;
        usedBucketBytes = 0;
        reservedBucketBytes = 0;
        bundleBytes = 0;
        ocsgwStreamId = null;
        ocsgwRequestId = null;
        serviceIdentifier = 0;
        ratingGroup = 0;
        messageType = null;
    }

    //FixMe : We need to think about roaming!!!

    public void update(
            final PrimeEventMessageType messageType,
            final String msisdn,
            final long requestedBytes,
            final long usedBytes,
            final long reservedBucketBytes,
            final long serviceIdentifier,
            final long ratingGroup,
            final String ocsgwStreamId,
            final String ocsgwRequestId) {
        this.messageType = messageType;
        this.msisdn = msisdn;
        this.requestedBucketBytes = requestedBytes;
        this.usedBucketBytes = usedBytes;
        this.reservedBucketBytes = reservedBucketBytes;
        this.serviceIdentifier = serviceIdentifier;
        this.ratingGroup = ratingGroup;
        this.ocsgwStreamId = ocsgwStreamId;
        this.ocsgwRequestId = ocsgwRequestId;
    }
}
