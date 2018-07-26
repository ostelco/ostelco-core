package org.ostelco.prime.ocs

import com.lmax.disruptor.EventHandler
import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.FinalUnitAction
import org.ostelco.ocs.api.FinalUnitIndication
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.ocs.api.ReportingReason
import org.ostelco.ocs.api.ServiceUnit
import org.ostelco.prime.disruptor.PrimeEvent
import org.ostelco.prime.disruptor.PrimeEventMessageType
import org.ostelco.prime.logger

/**
 * An event handler, handling the [PrimeEvent] messages that
 * are used by the Disruptor execution mechanism to handle events.
 */
internal class EventHandlerImpl(private val ocsService: OcsService) : EventHandler<PrimeEvent> {

    private val logger by logger()

    override fun onEvent(
            event: PrimeEvent,
            sequence: Long,
            endOfBatch: Boolean) {

        try {
            dispatchOnEventType(event)
        } catch (e: Exception) {
            logger.warn("Exception handling prime event in OcsService", e)
            // XXX Should the exception be cast further up the call chain?
        }

    }

    private fun dispatchOnEventType(event: PrimeEvent) {
        when (event.messageType) {
            PrimeEventMessageType.CREDIT_CONTROL_REQUEST -> handleCreditControlRequest(event)

            PrimeEventMessageType.TOPUP_DATA_BUNDLE_BALANCE -> handleTopupDataBundleBalance(event)

            else -> logger.warn("Unknown event type " + event.messageType!!)
        }
    }

    private fun handleTopupDataBundleBalance(event: PrimeEvent) {
        val response = ActivateResponse.newBuilder().setMsisdn(event.msisdn).build()
        ocsService.activateOnNextResponse(response)
    }

    private fun logEventProcessing(msg: String, event: PrimeEvent) {
        logger.info("{}", msg)
        logger.info("MSISDN: {}", event.msisdn)
        logger.info("requested bytes: {}", event.requestedBucketBytes)
        logger.info("reserved bytes: {}", event.reservedBucketBytes)
        logger.info("used bytes: {}", event.usedBucketBytes)
        logger.info("bundle bytes: {}", event.bundleBytes)
        logger.info("Reporting reason: {}", event.reportingReason)
        logger.info("request id: {} ",event.ocsgwRequestId)
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
                val msccBuilder = MultipleServiceCreditControl.newBuilder()
                msccBuilder.setServiceIdentifier(event.serviceIdentifier)
                        .setRatingGroup(event.ratingGroup)
                        .setValidityTime(86400)

                if ((event.reportingReason != ReportingReason.FINAL) && (event.requestedBucketBytes > 0)) {
                    msccBuilder.granted = ServiceUnit.newBuilder()
                            .setTotalOctets(event.reservedBucketBytes)
                            .build()
                    if (event.reservedBucketBytes < event.requestedBucketBytes) {
                        msccBuilder.finalUnitIndication = FinalUnitIndication.newBuilder()
                                .setFinalUnitAction(FinalUnitAction.TERMINATE)
                                .setIsSet(true)
                                .build()
                    }
                } else {
                    // Use -1 to indicate no granted service unit should be included in the answer
                    msccBuilder.granted = ServiceUnit.newBuilder()
                            .setTotalOctets(-1)
                            .build()
                }
                creditControlAnswer.addMscc(msccBuilder.build())
            }

            val streamId = event.ocsgwStreamId
            if (streamId != null) {
                ocsService.sendCreditControlAnswer(streamId, creditControlAnswer.build())
            }
        } catch (e: Exception) {
            logger.warn("Exception handling prime event", e)
            logEventProcessing("Exception sending Credit-Control-Answer", event)

            // unable to send Credit-Control-Answer.
            // So, return reserved bucket bytes back to data bundle.
            ocsService.returnUnusedDataBucketEvent(
                    event.msisdn!!, // TODO need proper null check
                    event.reservedBucketBytes)
        }

    }
}
