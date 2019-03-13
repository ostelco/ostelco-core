package org.ostelco.ocsgw.datasource.protobuf

import org.jdiameter.api.IllegalDiameterStateException
import org.jdiameter.api.InternalException
import org.jdiameter.api.OverloadException
import org.jdiameter.api.RouteException
import org.jdiameter.api.cca.ServerCCASession
import org.ostelco.diameter.CreditControlContext
import org.ostelco.diameter.getLogger
import org.ostelco.diameter.model.CreditControlAnswer
import org.ostelco.diameter.model.CreditControlRequest
import org.ostelco.diameter.model.MultipleServiceCreditControl
import org.ostelco.diameter.model.ResultCode.DIAMETER_UNABLE_TO_COMPLY
import org.ostelco.diameter.model.SessionContext
import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.CreditControlRequestType
import org.ostelco.ocsgw.OcsServer
import org.ostelco.ocsgw.converter.ProtobufToDiameterConverter
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReport
import org.ostelco.prime.metrics.api.User
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ProtobufDataSource {

    private val logger by getLogger()

    private val blocked = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private val ccrMap = ConcurrentHashMap<String, CreditControlContext>()

    private val sessionIdMap = ConcurrentHashMap<String, SessionContext>()

    fun handleRequest(context: CreditControlContext): CreditControlRequestInfo? {

        logger.info("[>>] creditControlRequest for {}", context.creditControlRequest.msisdn)

        // FixMe: We should handle conversion errors
        val creditControlRequestInfo = ProtobufToDiameterConverter.convertRequestToGrpc(context)
        if (creditControlRequestInfo != null) {
            ccrMap[context.sessionId] = context
            addToSessionMap(context)
            return creditControlRequestInfo
        }
        return null
    }

    fun handleProtobufCcrAnswer(answer: CreditControlAnswerInfo) {
        try {
            logger.info("[<<] CreditControlAnswer for {}", answer.msisdn)
            val ccrContext = ccrMap.remove(answer.requestId)
            if (ccrContext != null) {
                val session = OcsServer.stack?.getSession(ccrContext.sessionId, ServerCCASession::class.java)
                if (session != null && session.isValid) {
                    removeFromSessionMap(ccrContext)
                    updateBlockedList(answer, ccrContext.creditControlRequest)
                    if (!ccrContext.skipAnswer) {
                        val cca = createCreditControlAnswer(answer)
                        try {
                            session.sendCreditControlAnswer(ccrContext.createCCA(cca))
                        } catch (e: InternalException) {
                            logger.error("Failed to send Credit-Control-Answer", e)
                        } catch (e: IllegalDiameterStateException) {
                            logger.error("Failed to send Credit-Control-Answer", e)
                        } catch (e: RouteException) {
                            logger.error("Failed to send Credit-Control-Answer", e)
                        } catch (e: OverloadException) {
                            logger.error("Failed to send Credit-Control-Answer", e)
                        }

                    }
                } else {
                    logger.warn("No stored CCR or Session for {}", answer.requestId)
                }
            } else {
                logger.warn("Missing CreditControlContext for req id {}", answer.requestId)
            }
        } catch (e: Exception) {
            logger.error("Credit-Control-Request failed ", e)
        }
    }

    fun handleProtobufActivateResponse(activateResponse : ActivateResponse) {

        logger.info("Active user {}", activateResponse.msisdn)

        if (sessionIdMap.containsKey(activateResponse.msisdn)) {
            val sessionContext = sessionIdMap[activateResponse.msisdn]
            OcsServer.sendReAuthRequest(sessionContext)
        } else {
            logger.debug("No session context stored for msisdn : {}", activateResponse.msisdn)
        }
    }

    private fun addToSessionMap(creditControlContext: CreditControlContext) {
        try {
            val sessionContext = SessionContext(creditControlContext.sessionId,
                    creditControlContext.creditControlRequest.originHost,
                    creditControlContext.creditControlRequest.originRealm,
                    creditControlContext.creditControlRequest.serviceInformation[0].psInformation[0].calledStationId,
                    creditControlContext.creditControlRequest.serviceInformation[0].psInformation[0].sgsnMccMnc)
            sessionIdMap[creditControlContext.creditControlRequest.msisdn] = sessionContext
        } catch (e: Exception) {
            logger.error("Failed to update session map", e)
        }

    }

    private fun removeFromSessionMap(creditControlContext: CreditControlContext) {
        if (ProtobufToDiameterConverter.getRequestType(creditControlContext) == CreditControlRequestType.TERMINATION_REQUEST) {
            sessionIdMap.remove(creditControlContext.creditControlRequest.msisdn)
        }
    }

    fun getAnalyticsReport(): OcsgwAnalyticsReport {
        val builder = OcsgwAnalyticsReport.newBuilder().setActiveSessions(sessionIdMap.size)
        builder.keepAlive = false
        sessionIdMap.forEach { msisdn, (_, _, _, apn, mccMnc) -> builder.addUsers(User.newBuilder().setApn(apn).setMccMnc(mccMnc).setMsisdn(msisdn).build()) }
        return builder.build()
    }

    /**
     * A user will be blocked if one of the MSCC in the request could not be filled in the answer
     */
    private fun updateBlockedList(answer: CreditControlAnswerInfo, request: CreditControlRequest) {
        for (msccAnswer in answer.msccList) {
            for (msccRequest in request.multipleServiceCreditControls) {
                if (msccAnswer.serviceIdentifier == msccRequest.serviceIdentifier && msccAnswer.ratingGroup == msccRequest.ratingGroup) {
                    if (updateBlockedList(msccAnswer, msccRequest, answer.msisdn)) {
                        return
                    }
                }
            }
        }
    }

    private fun updateBlockedList(msccAnswer: org.ostelco.ocs.api.MultipleServiceCreditControl, msccRequest: MultipleServiceCreditControl, msisdn: String): Boolean {
        if (!msccRequest.requested.isEmpty()) {
            if (msccAnswer.granted.totalOctets < msccRequest.requested[0].total) {
                blocked.add(msisdn)
                return true
            } else {
                blocked.remove(msisdn)
            }
        }
        return false
    }

    private fun createCreditControlAnswer(response: CreditControlAnswerInfo?): CreditControlAnswer {
        if (response == null) {
            logger.error("Empty CreditControlAnswerInfo received")
            return CreditControlAnswer(DIAMETER_UNABLE_TO_COMPLY, ArrayList())
        }

        val multipleServiceCreditControls = LinkedList<MultipleServiceCreditControl>()
        for (mscc in response.msccList) {
            multipleServiceCreditControls.add(ProtobufToDiameterConverter.convertMSCC(mscc))
        }
        return CreditControlAnswer(ProtobufToDiameterConverter.convertResultCode(response.resultCode), multipleServiceCreditControls)
    }


    fun isBlocked(msisdn: String): Boolean {
        return blocked.contains(msisdn)
    }

}