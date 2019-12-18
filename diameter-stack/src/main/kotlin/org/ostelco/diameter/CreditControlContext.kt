package org.ostelco.diameter

import org.jdiameter.api.Avp
import org.jdiameter.api.AvpSet
import org.jdiameter.api.InternalException
import org.jdiameter.api.Request
import org.jdiameter.api.cca.events.JCreditControlRequest
import org.jdiameter.common.impl.app.cca.JCreditControlAnswerImpl
import org.ostelco.diameter.model.CreditControlAnswer
import org.ostelco.diameter.model.CreditControlRequest
import org.ostelco.diameter.model.FinalUnitAction
import org.ostelco.diameter.model.FinalUnitIndication
import org.ostelco.diameter.model.MultipleServiceCreditControl
import org.ostelco.diameter.model.RequestType
import org.ostelco.diameter.model.RequestType.TERMINATION_REQUEST
import org.ostelco.diameter.parser.AvpParser
import org.ostelco.diameter.util.DiameterUtilities

/**
 * @param sessionId
 * @param originalCreditControlRequest
 * @param originHost
 * @param originRealm
 */
class CreditControlContext(
        val sessionId: String,
        val originalCreditControlRequest: JCreditControlRequest,
        private val originHost: String,
        private val originRealm: String) {

    private val logger by getLogger()

    private val VENDOR_ID_3GPP = 10415L

    // Set to true, when answer to not to be sent to P-GW. This logic is used by ProxyDatasource.
    var skipAnswer: Boolean = false

    private val contextCreatedTime = System.currentTimeMillis()

    var sentToOcsTime: Long = 0L

    val creditControlRequest: CreditControlRequest = AvpParser().parse(
            CreditControlRequest::class,
            originalCreditControlRequest.message.avps)

    init {
        DiameterUtilities().printAvps(originalCreditControlRequest.message.avps)
    }

    fun createCCA(creditControlAnswer: CreditControlAnswer): JCreditControlAnswerImpl? {
        var answer: JCreditControlAnswerImpl? = null

        try {
            answer = JCreditControlAnswerImpl(originalCreditControlRequest.message as Request, creditControlAnswer.resultCode.value.toLong())

            val ccaAvps = answer.message.avps

            ccaAvps.addAvp(creditControlRequest.ccRequestType)
            ccaAvps.addAvp(creditControlRequest.ccRequestNumber)

            ccaAvps.addAvp(Avp.ORIGIN_HOST, originHost, true, false, true)
            ccaAvps.addAvp(Avp.ORIGIN_REALM, originRealm, true, false, true)

            if (!creditControlAnswer.multipleServiceCreditControls.isEmpty()) {
                addMultipleServiceCreditControls(ccaAvps, creditControlAnswer)
            } else if (originalCreditControlRequest.requestTypeAVPValue != TERMINATION_REQUEST) {
                ccaAvps.addAvp(Avp.VALIDITY_TIME, creditControlAnswer.validityTime, true, false)
            }

            DiameterUtilities().printAvps(ccaAvps)

        } catch (e: InternalException) {
            logger.error("Failed to convert to Credit-Control-Answer", e)
        }

        return answer
    }

    private fun addMultipleServiceCreditControls(ccaAvps: AvpSet, creditControlAnswer: CreditControlAnswer) {
        for (mscc in creditControlAnswer.multipleServiceCreditControls) {

            val answerMSCC = ccaAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL, true, false)
            if (mscc.ratingGroup > 0) {
                answerMSCC.addAvp(Avp.RATING_GROUP, mscc.ratingGroup, true, false, true)
            }

            if (mscc.serviceIdentifier > 0) {
                // This is a bug in jDiameter due to which this unsigned32 field has to be set as Int and not Long.
                answerMSCC.addAvp(Avp.SERVICE_IDENTIFIER_CCA, mscc.serviceIdentifier.toInt(), true, false)
            }

            if (originalCreditControlRequest.requestTypeAVPValue != RequestType.TERMINATION_REQUEST) {

                if (mscc.finalUnitIndication != null) {
                    addFinalUnitAction(answerMSCC, mscc)
                }

                if (mscc.granted.total > -1) {
                    val gsuAvp = answerMSCC.addGroupedAvp(Avp.GRANTED_SERVICE_UNIT, true, false)
                    //gsuAvp.addAvp(Avp.CC_INPUT_OCTETS, 0L, true, false)
                    //gsuAvp.addAvp(Avp.CC_OUTPUT_OCTETS, 0L, true, false)
                    gsuAvp.addAvp(Avp.CC_TOTAL_OCTETS, mscc.granted.total, true, false)
                }
            }

            answerMSCC.addAvp(Avp.RESULT_CODE, mscc.resultCode.value, true, false)
            answerMSCC.addAvp(Avp.VALIDITY_TIME, mscc.validityTime, true, false)

            if (mscc.granted.total > 0) {
                answerMSCC.addAvp(Avp.QUOTA_HOLDING_TIME, mscc.quotaHoldingTime, VENDOR_ID_3GPP, true, false, true)
                answerMSCC.addAvp(Avp.VOLUME_QUOTA_THRESHOLD, mscc.volumeQuotaThreshold, VENDOR_ID_3GPP,true, false, true)
            }
        }
    }

    private fun addFinalUnitAction(answerMSCC: AvpSet, mscc: MultipleServiceCreditControl) {

        // There seems to be a possibility to do some whitelisting here by using RESTRICT_ACCESS
        // We should have a look at: https://tools.ietf.org/html/rfc4006#section-5.6.3

        val originalFinalUnitIndication: FinalUnitIndication? = mscc.finalUnitIndication
        if (originalFinalUnitIndication != null) {
            val finalUnitIndication = answerMSCC.addGroupedAvp(Avp.FINAL_UNIT_INDICATION, true, false)
            finalUnitIndication.addAvp(Avp.FINAL_UNIT_ACTION, originalFinalUnitIndication.finalUnitAction.ordinal, true, false)
            if (originalFinalUnitIndication.finalUnitAction == FinalUnitAction.REDIRECT) {
                val originalRedirectServer = originalFinalUnitIndication.redirectServer
                if (originalRedirectServer != null) {
                    val redirectServer = finalUnitIndication.addGroupedAvp(Avp.REDIRECT_SERVER, true, false)
                    redirectServer.addAvp(Avp.REDIRECT_ADDRESS_TYPE, originalRedirectServer.redirectAddressType.ordinal, true, false)
                    redirectServer.addAvp(Avp.REDIRECT_ADDRESS, originalRedirectServer.redirectServerAddress, true, false, false)
                }
            } else if (originalFinalUnitIndication.finalUnitAction == FinalUnitAction.RESTRICT_ACCESS) {
                for (restrictionFilerRule in originalFinalUnitIndication.restrictionFilterRule) {
                    finalUnitIndication.addAvp(Avp.RESTRICTION_FILTER_RULE, restrictionFilerRule, true, false, true)
                }
            }
        }
    }

    fun logLatency() {
        logger.info("Time from request to answer {} ms. OCS roundtrip {} ms. SessionId [{}] request number [{}]", System.currentTimeMillis() - contextCreatedTime, System.currentTimeMillis() - sentToOcsTime, this.sessionId, this.creditControlRequest.ccRequestNumber?.unsigned32)
    }
}
