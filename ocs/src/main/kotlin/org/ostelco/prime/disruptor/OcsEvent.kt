package org.ostelco.prime.disruptor

import org.ostelco.ocs.api.CreditControlRequestInfo

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
     * Credit-Control-Request from OCS
     */
    var request: CreditControlRequestInfo? = null

    var topupContext: TopupContext? = null

    fun clear() {
        messageType = null
        msisdn = null
        bundleId = null
        bundleBytes = 0
        reservedBucketBytes = 0
        ocsgwStreamId = null
        request = null
        topupContext = null;
    }

    //FIXME vihang: We need to think about roaming

    fun update(
            messageType: EventMessageType?,
            msisdn: String?,
            bundleId: String?,
            bundleBytes: Long,
            reservedBucketBytes: Long,
            ocsgwStreamId: String?,
            request: CreditControlRequestInfo?,
            topupContext: TopupContext?) {
        this.messageType = messageType
        this.msisdn = msisdn
        this.bundleId = bundleId
        this.bundleBytes = bundleBytes
        this.reservedBucketBytes = reservedBucketBytes
        this.ocsgwStreamId = ocsgwStreamId
        this.request = request
        this.topupContext = topupContext
    }
}

class TopupContext(
        val requestId: String,
        val topUpBytes: Long,
        var msisdnToppedUp: List<String> = emptyList(),
        var errorMessage: String = "")