package org.ostelco.prime.ocs

import com.lmax.disruptor.EventHandler
import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.FinalUnitAction
import org.ostelco.ocs.api.FinalUnitIndication
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.ocs.api.ReportingReason
import org.ostelco.ocs.api.ServiceUnit
import org.ostelco.prime.disruptor.EventMessageType.CREDIT_CONTROL_REQUEST
import org.ostelco.prime.disruptor.EventMessageType.TOPUP_DATA_BUNDLE_BALANCE
import org.ostelco.prime.disruptor.OcsEvent
import org.ostelco.prime.logger

/**
 * An event handler, handling the [OcsEvent] messages that
 * are used by the Disruptor execution mechanism to handle events.
 */
internal class EventHandlerImpl(private val ocsService: OcsService) : EventHandler<OcsEvent> {

    private val logger by logger()

    override fun onEvent(
            event: OcsEvent,
            sequence: Long,
            endOfBatch: Boolean) {

        try {
            dispatchOnEventType(event)
        } catch (e: Exception) {
            logger.warn("Exception handling prime event in OcsService", e)
        }
    }

    private fun dispatchOnEventType(event: OcsEvent) {
        when (event.messageType) {
            CREDIT_CONTROL_REQUEST -> handleCreditControlRequest(event)
            TOPUP_DATA_BUNDLE_BALANCE -> handleTopupDataBundleBalance(event)

            else -> {} // do nothing
        }
    }

    private fun handleTopupDataBundleBalance(event: OcsEvent) {
        event.msisdnToppedUp?.forEach { msisdn ->
            val response = ActivateResponse.newBuilder().setMsisdn(msisdn).build()
            ocsService.activateOnNextResponse(response)
        }
    }

    private fun logEventProcessing(msg: String, event: OcsEvent) {
        val logString = """
            $msg
            Msisdn: ${event.msisdn}
            Requested bytes: ${event.request?.msccList?.firstOrNull()?.requested?.totalOctets ?: 0L}
            Used bytes: ${event.request?.msccList?.firstOrNull()?.used?.totalOctets ?: 0L}
            Bundle bytes: ${event.bundleBytes}
            Topup bytes: ${event.topUpBytes}
            Request id: ${event.request?.requestId}
        """.trimIndent()

        logger.info(logString)
    }

    private fun handleCreditControlRequest(event: OcsEvent) {

        logEventProcessing("Returning Credit-Control-Answer", event)

        // FIXME martin: This assume we only have one MSCC
        // TODO martin: In case of zero balance we should add appropriate FinalUnitAction

        try {
            val creditControlAnswer = CreditControlAnswerInfo.newBuilder()
                    .setMsisdn(event.msisdn)

            event.request?.let { request ->
                if (request.msccCount > 0) {
                    val msccBuilder = MultipleServiceCreditControl.newBuilder()
                    msccBuilder.setServiceIdentifier(request.getMscc(0).serviceIdentifier)
                            .setRatingGroup(request.getMscc(0).ratingGroup)
                            .setValidityTime(86400)

                    if ((request.getMscc(0).reportingReason != ReportingReason.FINAL) && (request.getMscc(0).requested.totalOctets > 0)) {
                        msccBuilder.granted = ServiceUnit.newBuilder()
                                .setTotalOctets(event.reservedBucketBytes)
                                .build()
                        if (event.reservedBucketBytes < request.getMscc(0).requested.totalOctets) {
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
                creditControlAnswer.setRequestId(request.requestId)
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
                    event.msisdn!!, // TODO vihang: need proper null check
                    event.reservedBucketBytes)
        }

    }
}
