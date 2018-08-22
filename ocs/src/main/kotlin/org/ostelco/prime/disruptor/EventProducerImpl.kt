package org.ostelco.prime.disruptor

import com.lmax.disruptor.RingBuffer
import org.ostelco.ocs.grpc.api.CreditControlRequestInfo
import org.ostelco.ocs.grpc.api.ReportingReason
import org.ostelco.prime.disruptor.EventMessageType.ADD_MSISDN_TO_BUNDLE_MAPPING
import org.ostelco.prime.disruptor.EventMessageType.CREDIT_CONTROL_REQUEST
import org.ostelco.prime.disruptor.EventMessageType.RELEASE_RESERVED_BUCKET
import org.ostelco.prime.disruptor.EventMessageType.TOPUP_DATA_BUNDLE_BALANCE
import org.ostelco.prime.disruptor.EventMessageType.UPDATE_BUNDLE
import org.ostelco.prime.logger
import org.ostelco.prime.model.Bundle
import java.util.function.Consumer

class EventProducerImpl(private val ringBuffer: RingBuffer<OcsEvent>) : EventProducer {

    private val logger by logger()

    private fun processNextEventOnTheRingBuffer(consumer: Consumer<OcsEvent>) {

        // pick
        val sequence = ringBuffer.next()
        try {
            val event = ringBuffer.get(sequence)

            // XXX If event == null, then we're a bit screwed.
            if (event == null) {
                // XXX Actually this is wrong, we're ignoring the update
                //     request since we couldn't get something out of the buffer
                //     altogether different and potentially a lost topup.
                logger.error("Dropping PrimeEvent update " + "since we couldn't get one off the ringbuffer")
                return
            }

            // Modify
            consumer.accept(event)

            // Publish
        } finally {  // XXX Why is the "finally" necessary here?
            ringBuffer.publish(sequence)
        }
    }

    private fun injectIntoRingBuffer(
            type: EventMessageType,
            msisdn: String? = null,
            bundleId: String? = null,
            bundleBytes: Long = 0,
            requestedBytes: Long = 0,
            usedBytes: Long = 0,
            reservedBytes: Long = 0,
            serviceId: Long = 0,
            ratingGroup: Long = 0,
            reportingReason: ReportingReason = ReportingReason.UNRECOGNIZED,
            streamId: String? = null,
            requestId: String? = null) {

        processNextEventOnTheRingBuffer(
                Consumer { event ->
                    event.update(type,
                            msisdn,
                            bundleId,
                            emptyList(),
                            bundleBytes,
                            requestedBytes,
                            usedBytes,
                            reservedBytes,
                            serviceId,
                            ratingGroup,
                            reportingReason,
                            streamId,
                            requestId)
                })
    }

    override fun topupDataBundleBalanceEvent(
            bundleId: String,
            bytes: Long) {

        injectIntoRingBuffer(
                type = TOPUP_DATA_BUNDLE_BALANCE,
                bundleId = bundleId,
                requestedBytes = bytes)
    }

    override fun releaseReservedDataBucketEvent(
            msisdn: String,
            bytes: Long) {

        injectIntoRingBuffer(
                type = RELEASE_RESERVED_BUCKET,
                msisdn = msisdn,
                requestedBytes = bytes)
    }

    override fun injectCreditControlRequestIntoRingbuffer(
            request: CreditControlRequestInfo,
            streamId: String) {

        if (request.msccList.isEmpty()) {
            injectIntoRingBuffer(CREDIT_CONTROL_REQUEST,
                    request.msisdn,
                    streamId = streamId,
                    requestId = request.requestId)
        } else {
            // FIXME vihang: For now we assume that there is only 1 MSCC in the Request.
            injectIntoRingBuffer(CREDIT_CONTROL_REQUEST,
                    msisdn = request.msisdn,
                    requestedBytes = request.getMscc(0).requested.totalOctets,
                    usedBytes = request.getMscc(0).used.totalOctets,
                    reservedBytes = 0,
                    serviceId = request.getMscc(0).serviceIdentifier,
                    ratingGroup = request.getMscc(0).ratingGroup,
                    reportingReason = request.getMscc(0).reportingReason,
                    streamId = streamId,
                    requestId = request.requestId)
        }
    }

    override fun addBundle(bundle: Bundle) {
        injectIntoRingBuffer(UPDATE_BUNDLE, bundleId = bundle.id, bundleBytes = bundle.balance)
    }

    override fun addMsisdnToBundleMapping(msisdn: String, bundleId: String) {
        injectIntoRingBuffer(ADD_MSISDN_TO_BUNDLE_MAPPING, msisdn = msisdn, bundleId = bundleId)
    }
}
