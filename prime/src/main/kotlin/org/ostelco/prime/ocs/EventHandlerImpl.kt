package org.ostelco.prime.ocs

import com.lmax.disruptor.EventHandler
import org.ostelco.ocs.api.*
import org.ostelco.prime.disruptor.PrimeEvent
import org.ostelco.prime.disruptor.PrimeEventMessageType
import org.ostelco.prime.logger

/**
 * An event handler, handling the [PrimeEvent] messages that
 * are used by the Disruptor execution mechanism to handle events.
 */
internal class EventHandlerImpl(private val ocsService: OcsService) : EventHandler<PrimeEvent> {

    private val LOG by logger()

    override fun onEvent(
            event: PrimeEvent,
            sequence: Long,
            endOfBatch: Boolean) {

        try {
            dispatchOnEventType(event)
        } catch (e: Exception) {
            LOG.warn("Exception handling prime event in OcsService", e)
            // XXX Should the exception be cast further up the call chain?
        }

    }

    private fun dispatchOnEventType(event: PrimeEvent) {
        when (event.messageType) {
            PrimeEventMessageType.CREDIT_CONTROL_REQUEST -> handleCreditControlRequest(event)

            PrimeEventMessageType.TOPUP_DATA_BUNDLE_BALANCE -> handleTopupDataBundleBalance(event)

            else -> LOG.warn("Unknown event type " + event.messageType!!)
        }
    }

    private fun handleTopupDataBundleBalance(event: PrimeEvent) {
        val response = ActivateResponse.newBuilder().setMsisdn(event.msisdn).build()
        ocsService.activateOnNextResponse(response)
    }

    private fun logEventProcessing(msg: String, event: PrimeEvent) {
        LOG.info("{}", msg);
        LOG.info("MSISDN: {}", event.msisdn);
        LOG.info("requested bytes: {}", event.requestedBucketBytes);
        LOG.info("reserved bytes: {}", event.reservedBucketBytes);
        LOG.info("used bytes: {}", event.usedBucketBytes);
        LOG.info("bundle bytes: {}", event.bundleBytes);
        LOG.info("Reporting reason: {}", event.reportingReason);
        LOG.info("request id: {} ",event.ocsgwRequestId);

    }

    private fun handleCreditControlRequest(event: PrimeEvent) {

        logEventProcessing("Returning Credit-Control-Answer", event)

        // FixMe : This assume we only have one MSCC
        // ToDo : In case of zero balance we should add appropriate FinalUnitAction

        try {
            val creditControlAnswer = CreditControlAnswerInfo.newBuilder()
                    .setMsisdn(event.msisdn)
                    .setRequestId(event.ocsgwRequestId)

            // This is a hack to know when we have received an MSCC in the request or not.
            // For Terminate request we might not have any MSCC and therefore no serviceIdentifier.
            if (event.serviceIdentifier > 0) {
                val msccBulder = MultipleServiceCreditControl.newBuilder()
                msccBulder.setServiceIdentifier(event.serviceIdentifier)
                        .setRatingGroup(event.ratingGroup)
                        .setValidityTime(86400)

                if ((event.reportingReason != ReportingReason.FINAL) && (event.requestedBucketBytes > 0)) {
                    msccBulder.setGranted(ServiceUnit.newBuilder()
                            .setTotalOctets(event.reservedBucketBytes)
                            .build())
                    if (event.reservedBucketBytes < event.requestedBucketBytes) {
                        msccBulder.setFinalUnitIndication(FinalUnitIndication.newBuilder()
                                .setFinalUnitAction(FinalUnitAction.TERMINATE)
                                .setIsSet(true)
                                .build())
                    }
                }
                creditControlAnswer.addMscc(msccBulder.build())
            }

            ocsService.sendCreditControlAnswer(event.ocsgwStreamId ?: "", creditControlAnswer.build())
        } catch (e: Exception) {
            LOG.warn("Exception handling prime event", e)
            logEventProcessing("Exception sending Credit-Control-Answer", event)

            // unable to send Credit-Control-Answer.
            // So, return reserved bucket bytes back to data bundle.
            ocsService.returnUnusedDataBucketEvent(
                    event.msisdn!!, // TODO need proper null check
                    event.reservedBucketBytes)
        }

    }
}
