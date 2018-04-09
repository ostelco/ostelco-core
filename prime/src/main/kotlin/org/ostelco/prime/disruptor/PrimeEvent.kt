package org.ostelco.prime.disruptor

class PrimeEvent {

    /**
     * The type of message this is, closely mirroring the types of messages in
     * ocs.proto (the GRPC specification file).
     */
    var messageType: PrimeEventMessageType? = null

    /**
     * Phone number this event is related to.
     */
    var msisdn: String? = null

    /**
     * Origin of word 'bucket' - P-GW consumes data in `buckets` of 10 MB ~ 100 MB at a time
     * This field is used in.
     * Request to reserve a new bucket of bytes
     */
    var requestedBucketBytes: Long = 0

    /**
     * Bytes that has been used from the bucket (previously reserved).
     */
    var usedBucketBytes: Long = 0


    /**
     * Buckets that has been reserved from the bundle.
     */
    var reservedBucketBytes: Long = 0

    /**
     * Origin of word 'bundle' - Subscriber buys a 'bundle' of units of data/airtime/validity etc.
     * This field represent total balance bytes.
     */
    var bundleBytes: Long = 0

    /**
     * Stream ID used to correlate response with correct gRPC stream.
     */
    var ocsgwStreamId: String? = null

    /**
     * Request ID used by OCS gateway to correlate response with requests
     */
    var ocsgwRequestId: String? = null


    /**
     * Service-Identifier is used to classify traffic
     */
    var serviceIdentifier: Long = 0

    /**
     * Rating-Group is used to classify traffic
     */
    var ratingGroup: Long = 0

    fun clear() {
        msisdn = null
        requestedBucketBytes = 0
        usedBucketBytes = 0
        reservedBucketBytes = 0
        bundleBytes = 0
        ocsgwStreamId = null
        ocsgwRequestId = null
        serviceIdentifier = 0
        ratingGroup = 0
        messageType = null
    }

    //FixMe : We need to think about roaming!!!

    fun update(
            messageType: PrimeEventMessageType?,
            msisdn: String?,
            requestedBytes: Long,
            usedBytes: Long,
            reservedBucketBytes: Long,
            serviceIdentifier: Long,
            ratingGroup: Long,
            ocsgwStreamId: String?,
            ocsgwRequestId: String?) {
        this.messageType = messageType
        this.msisdn = msisdn
        this.requestedBucketBytes = requestedBytes
        this.usedBucketBytes = usedBytes
        this.reservedBucketBytes = reservedBucketBytes
        this.serviceIdentifier = serviceIdentifier
        this.ratingGroup = ratingGroup
        this.ocsgwStreamId = ocsgwStreamId
        this.ocsgwRequestId = ocsgwRequestId
    }
}
