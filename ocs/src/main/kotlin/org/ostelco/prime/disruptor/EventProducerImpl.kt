package org.ostelco.prime.disruptor

import com.lmax.disruptor.RingBuffer
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.ReportingReason
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
            reservedBytes: Long = 0,
            streamId: String? = null,
            request: CreditControlRequestInfo? = null,
            topUpBytes: Long? = 0) {

        processNextEventOnTheRingBuffer(
                Consumer { event ->
                    event.update(type,
                            msisdn,
                            bundleId,
                            emptyList(),
                            bundleBytes,
                            reservedBytes,
                            streamId,
                            request,
                            topUpBytes)
                })
    }

    override fun topupDataBundleBalanceEvent(
            bundleId: String,
            bytes: Long) {

        injectIntoRingBuffer(
                type = TOPUP_DATA_BUNDLE_BALANCE,
                bundleId = bundleId,
                topUpBytes = bytes)
    }

    override fun releaseReservedDataBucketEvent(
            msisdn: String,
            bytes: Long) {

        injectIntoRingBuffer(
                type = RELEASE_RESERVED_BUCKET,
                msisdn = msisdn)
    }

    override fun injectCreditControlRequestIntoRingbuffer(
            request: CreditControlRequestInfo,
            streamId: String) {

        injectIntoRingBuffer(CREDIT_CONTROL_REQUEST,
                msisdn = request.msisdn,
                reservedBytes = 0,
                streamId = streamId,
                request = request)
    }

    override fun addBundle(bundle: Bundle) {
        injectIntoRingBuffer(UPDATE_BUNDLE, bundleId = bundle.id, bundleBytes = bundle.balance)
    }

    override fun addMsisdnToBundleMapping(msisdn: String, bundleId: String) {
        injectIntoRingBuffer(ADD_MSISDN_TO_BUNDLE_MAPPING, msisdn = msisdn, bundleId = bundleId)
    }
}
