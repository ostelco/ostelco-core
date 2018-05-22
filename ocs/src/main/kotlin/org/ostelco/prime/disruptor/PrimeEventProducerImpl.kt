package org.ostelco.prime.disruptor

import com.google.common.base.Preconditions.checkNotNull
import com.lmax.disruptor.RingBuffer
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.ReportingReason
import org.ostelco.prime.disruptor.PrimeEventMessageType.CREDIT_CONTROL_REQUEST
import org.ostelco.prime.disruptor.PrimeEventMessageType.RELEASE_RESERVED_BUCKET
import org.ostelco.prime.disruptor.PrimeEventMessageType.TOPUP_DATA_BUNDLE_BALANCE
import org.ostelco.prime.logger
import java.util.function.Consumer

class PrimeEventProducerImpl(private val ringBuffer: RingBuffer<PrimeEvent>) : PrimeEventProducer {

    private val LOG by logger()

    private fun processNextEventOnTheRingbuffer(consumer: Consumer<PrimeEvent>) {
        checkNotNull(consumer)

        // pick
        val sequence = ringBuffer.next()
        try {
            val event = ringBuffer.get(sequence)

            // XXX If event == null, then we're a bit screwed.
            if (event == null) {
                // XXX Actually this is wrong, we're ignoring the update
                //     request since we couldn't get something out of the buffer
                //     altogether different and potentially a lost topup.
                LOG.error("Dropping PrimeEvent update " + "since we couldn't get one off the ringbuffer")
                return
            }

            // Modify
            consumer.accept(event)

            // Publish
        } finally {  // XXX Why is the "finally" necessary here?
            ringBuffer.publish(sequence)
        }
    }

    private fun injectIntoRingbuffer(
            type: PrimeEventMessageType,
            msisdn: String,
            requestedBytes: Long = 0,
            usedBytes: Long = 0,
            reservedBytes: Long = 0,
            serviceId: Long = 0,
            ratingGroup: Long = 0,
            reportingReason: ReportingReason = ReportingReason.UNRECOGNIZED,
            streamId: String? = null,
            requestId: String? = null) {

        processNextEventOnTheRingbuffer(
                Consumer { event ->
                    event.update(type,
                            msisdn,
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
            msisdn: String,
            bytes: Long) {

        injectIntoRingbuffer(
                type = TOPUP_DATA_BUNDLE_BALANCE,
                msisdn = msisdn,
                requestedBytes = bytes)
    }

    override fun releaseReservedDataBucketEvent(
            msisdn: String,
            bytes: Long) {

        injectIntoRingbuffer(
                type = RELEASE_RESERVED_BUCKET,
                msisdn = msisdn,
                requestedBytes = bytes)
    }

    // FixMe : For now we assume that there is only 1 MSCC in the Request.
    override fun injectCreditControlRequestIntoRingbuffer(
            request: CreditControlRequestInfo,
            streamId: String) {

        if (request.msccList.isEmpty()) {
            injectIntoRingbuffer(CREDIT_CONTROL_REQUEST,
                    request.msisdn,
                    streamId = streamId,
                    requestId = request.requestId)
        } else {
            injectIntoRingbuffer(CREDIT_CONTROL_REQUEST,
                    request.msisdn,
                    request.getMscc(0).requested.totalOctets,
                    request.getMscc(0).used.totalOctets,
                    0,
                    request.getMscc(0).serviceIdentifier,
                    request.getMscc(0).ratingGroup,
                    request.getMscc(0).reportingReason,
                    streamId,
                    request.requestId)
        }
    }
}
