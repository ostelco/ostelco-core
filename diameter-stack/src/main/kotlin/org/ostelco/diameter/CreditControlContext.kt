package org.ostelco.diameter

import org.jdiameter.api.Avp
import org.jdiameter.api.AvpSet
import org.jdiameter.api.InternalException
import org.jdiameter.api.Request
import org.jdiameter.api.ResultCode
import org.jdiameter.api.cca.events.JCreditControlRequest
import org.jdiameter.common.impl.app.cca.JCreditControlAnswerImpl
import org.ostelco.diameter.model.CreditControlAnswer
import org.ostelco.diameter.model.CreditControlRequest
import org.ostelco.diameter.model.FinalUnitAction
import org.ostelco.diameter.model.FinalUnitIndication
import org.ostelco.diameter.model.MultipleServiceCreditControl
import org.ostelco.diameter.model.RequestType
import org.ostelco.diameter.parser.AvpParser
import org.ostelco.diameter.util.DiameterUtilities

/**
 * @param sessionId
 * @param originalCreditControlRequest
 * @param originHost
 * @param skipAnswer Set to true, when answer to not to be sent to PGw. Default value is false.
 */
class CreditControlContext(
        val sessionId: String,
        val originalCreditControlRequest: JCreditControlRequest,
        val originHost: String) {

    private val LOG by logger()

    var skipAnswer: Boolean = false

    val originRealm:String = originalCreditControlRequest.destinationRealm

    val creditControlRequest: CreditControlRequest = AvpParser().parse(
            CreditControlRequest::class,
            originalCreditControlRequest.message.avps)

    init {
        DiameterUtilities().printAvps(originalCreditControlRequest.message.avps)
    }

    fun createCCA(creditControlAnswer: CreditControlAnswer): JCreditControlAnswerImpl? {

        var answer: JCreditControlAnswerImpl? = null
        var resultCode = ResultCode.SUCCESS

        try {
            answer = JCreditControlAnswerImpl(originalCreditControlRequest.message as Request, ResultCode.SUCCESS.toLong())

            val ccaAvps = answer.message.avps

            ccaAvps.addAvp(creditControlRequest.ccRequestType)
            ccaAvps.addAvp(creditControlRequest.ccRequestNumber)

            ccaAvps.addAvp(Avp.ORIGIN_HOST, originHost, true, false, true)
            ccaAvps.addAvp(Avp.ORIGIN_REALM, originRealm, true, false, true)

            val multipleServiceCreditControls = creditControlAnswer.multipleServiceCreditControls

            for (mscc in multipleServiceCreditControls) {

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
                        gsuAvp.addAvp(Avp.CC_INPUT_OCTETS, 0L, true, false)
                        gsuAvp.addAvp(Avp.CC_OUTPUT_OCTETS, 0L, true, false)
                        gsuAvp.addAvp(Avp.CC_TOTAL_OCTETS, mscc.granted.total, true, false)
                    }
                }
                answerMSCC.addAvp(Avp.RESULT_CODE, resultCode, true, false)
                answerMSCC.addAvp(Avp.VALIDITY_TIME, mscc.validityTime, true, false)
            }
            LOG.info("Credit-Control-Answer")
            DiameterUtilities().printAvps(ccaAvps)

        } catch (e: InternalException) {
            LOG.error("Failed to convert to Credit-Control-Answer", e)
        }

        return answer
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
}
