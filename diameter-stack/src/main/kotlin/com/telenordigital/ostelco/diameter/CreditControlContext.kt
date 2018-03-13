package com.telenordigital.ostelco.diameter

import com.telenordigital.ostelco.diameter.model.CreditControlAnswer
import com.telenordigital.ostelco.diameter.model.CreditControlRequest
import com.telenordigital.ostelco.diameter.model.CreditControlResultCode
import com.telenordigital.ostelco.diameter.model.MultipleServiceCreditControl
import com.telenordigital.ostelco.diameter.model.RequestType
import com.telenordigital.ostelco.diameter.parser.CreditControlRequestParser
import com.telenordigital.ostelco.diameter.util.DiameterUtilities
import org.jdiameter.api.Avp
import org.jdiameter.api.AvpSet
import org.jdiameter.api.IllegalDiameterStateException
import org.jdiameter.api.InternalException
import org.jdiameter.api.OverloadException
import org.jdiameter.api.Request
import org.jdiameter.api.ResultCode
import org.jdiameter.api.RouteException
import org.jdiameter.api.cca.ServerCCASession
import org.jdiameter.api.cca.events.JCreditControlRequest
import org.jdiameter.common.impl.app.cca.JCreditControlAnswerImpl

class CreditControlContext(
        val session: ServerCCASession,
        val originalCreditControlRequest: JCreditControlRequest) {

    private val LOG by logger()

    private var sent: Boolean = false

    val creditControlRequest: CreditControlRequest = CreditControlRequestParser(originalCreditControlRequest).parse()

    private var originHost: String? = null
    private var originRealm: String? = null

    fun setOriginHost(fqdn: String) {
        originHost = fqdn
    }

    fun setOriginRealm(realmName: String) {
        originRealm = realmName
    }

    fun sendCreditControlAnswer(creditControlAnswer: CreditControlAnswer) {
        if (!sent) {
            sent = true;
            val cca = createCCA(creditControlAnswer)
            if (cca != null) {
                try {
                    session.sendCreditControlAnswer(cca)
                } catch (e: InternalException) {
                    LOG.error("Failed to send Credit-Control-Answer", e)
                } catch (e: IllegalDiameterStateException) {
                    LOG.error("Failed to send Credit-Control-Answer", e)
                } catch (e: RouteException) {
                    LOG.error("Failed to send Credit-Control-Answer", e)
                } catch (e: OverloadException) {
                    LOG.error("Failed to send Credit-Control-Answer", e)
                }
            }
        }
    }

    private fun createCCA(creditControlAnswer: CreditControlAnswer): JCreditControlAnswerImpl? {

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
                    answerMSCC.addAvp(Avp.RATING_GROUP, mscc.ratingGroup.toLong(), true, false, true)
                }
                if (mscc.serviceIdentifier > 0) {
                    answerMSCC.addAvp(Avp.SERVICE_IDENTIFIER_CCA, mscc.serviceIdentifier, true, false)
                }
                if (mscc.grantedServiceUnit < 1 && originalCreditControlRequest.requestTypeAVPValue != RequestType.TERMINATION_REQUEST) {
                    resultCode = CreditControlResultCode.DIAMETER_CREDIT_LIMIT_REACHED.value
                }

                val gsuAvp = answerMSCC.addGroupedAvp(Avp.GRANTED_SERVICE_UNIT, true, false)
                gsuAvp.addAvp(Avp.CC_INPUT_OCTETS, 0L, true, false)
                gsuAvp.addAvp(Avp.CC_OUTPUT_OCTETS, 0L, true, false)

                if (originalCreditControlRequest.requestTypeAVPValue == RequestType.TERMINATION_REQUEST || mscc.grantedServiceUnit < 1) {
                    LOG.info("Terminate")
                    // Since this is a terminate reply no service is granted
                    gsuAvp.addAvp(Avp.CC_TIME, 0, true, false)
                    gsuAvp.addAvp(Avp.CC_TOTAL_OCTETS, 0L, true, false)
                    gsuAvp.addAvp(Avp.CC_SERVICE_SPECIFIC_UNITS, 0L, true, false)

                    addFinalUnitAction(answerMSCC, mscc)
                } else {
                    gsuAvp.addAvp(Avp.CC_TOTAL_OCTETS, mscc.grantedServiceUnit, true, false)
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

        if (mscc.finalUnitIndication != null) {
            val finalUnitIndication = answerMSCC.addGroupedAvp(Avp.FINAL_UNIT_INDICATION, true, false)
            finalUnitIndication.addAvp(Avp.FINAL_UNIT_ACTION, mscc.finalUnitIndication.finalUnitAction.ordinal, true, false)
        }

        //ToDo : Add support for the rest of the Final-Unit-Action
    }
}
