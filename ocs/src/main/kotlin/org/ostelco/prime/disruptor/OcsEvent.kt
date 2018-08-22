package org.ostelco.prime.disruptor

import org.ostelco.ocs.grpc.api.ReportingReason

class OcsEvent {

    /**
     * The type of message this is, closely mirroring the types of messages in
     * ocs.proto (the GRPC specification file).
     */
    var messageType: EventMessageType? = null

    /**
     * Phone number this event is related to.
     */
    var msisdn: String? = null

    /**
     * Bundle ID of bundle to which bundleBytes are to be set.
     */
    var bundleId: String? = null

    /**
     * Phone numbers linked to a bundle which is topped up.
     */
    var msisdnToppedUp: List<String>? = null

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

    /**
     * Reporting-Reason
     * // FIXME martin: This is the Reporting-Reason for the MSCC. The PrimeEvent might be to generic since there is also Reporting-Reason used on ServiceUnit level
     */
    var reportingReason: ReportingReason = ReportingReason.UNRECOGNIZED

    fun clear() {
        messageType = null

        msisdn = null
        bundleId = null

        msisdnToppedUp = null

        bundleBytes = 0
        requestedBucketBytes = 0
        usedBucketBytes = 0
        reservedBucketBytes = 0
        bundleBytes = 0

        ocsgwStreamId = null
        ocsgwRequestId = null
        serviceIdentifier = 0
        ratingGroup = 0
        reportingReason = ReportingReason.UNRECOGNIZED
    }

    //FIXME vihang: We need to think about roaming!!!

    fun update(
            messageType: EventMessageType?,
            msisdn: String?,
            bundleId: String?,
            msisdnToppedUp: List<String>,
            bundleBytes: Long,
            requestedBytes: Long,
            usedBytes: Long,
            reservedBucketBytes: Long,
            serviceIdentifier: Long,
            ratingGroup: Long,
            reportingReason: ReportingReason,
            ocsgwStreamId: String?,
            ocsgwRequestId: String?) {
        this.messageType = messageType
        this.msisdn = msisdn
        this.bundleId = bundleId
        this.msisdnToppedUp = msisdnToppedUp
        this.bundleBytes = bundleBytes
        this.requestedBucketBytes = requestedBytes
        this.usedBucketBytes = usedBytes
        this.reservedBucketBytes = reservedBucketBytes
        this.serviceIdentifier = serviceIdentifier
        this.ratingGroup = ratingGroup
        this.reportingReason = reportingReason
        this.ocsgwStreamId = ocsgwStreamId
        this.ocsgwRequestId = ocsgwRequestId
    }
}
