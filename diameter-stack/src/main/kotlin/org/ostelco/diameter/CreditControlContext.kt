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
import org.ostelco.diameter.model.CreditControlResultCode
import org.ostelco.diameter.model.FinalUnitIndication
import org.ostelco.diameter.model.MultipleServiceCreditControl
import org.ostelco.diameter.model.RequestType
import org.ostelco.diameter.parser.AvpParser
import org.ostelco.diameter.util.DiameterUtilities

class CreditControlContext(
        val sessionId: String,
        val originalCreditControlRequest: JCreditControlRequest) {

    private val LOG by logger()

    private var sent: Boolean = false

    val originHost:String = originalCreditControlRequest.originHost
    val originRealm:String = originalCreditControlRequest.originRealm
    val destinationHost:String = originalCreditControlRequest.destinationHost
    val destinationRealm:String = originalCreditControlRequest.destinationRealm

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

            // We where the destination host/realm in the original request so in answer we switch
            ccaAvps.addAvp(Avp.ORIGIN_HOST, originalCreditControlRequest.destinationHost, true, false, true)
            ccaAvps.addAvp(Avp.ORIGIN_REALM, originalCreditControlRequest.destinationRealm, true, false, true)

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
                if (mscc.granted.total < 1 && originalCreditControlRequest.requestTypeAVPValue != RequestType.TERMINATION_REQUEST) {
                    resultCode = CreditControlResultCode.DIAMETER_CREDIT_LIMIT_REACHED.value
                }

                val gsuAvp = answerMSCC.addGroupedAvp(Avp.GRANTED_SERVICE_UNIT, true, false)
                gsuAvp.addAvp(Avp.CC_INPUT_OCTETS, 0L, true, false)
                gsuAvp.addAvp(Avp.CC_OUTPUT_OCTETS, 0L, true, false)

                if (originalCreditControlRequest.requestTypeAVPValue == RequestType.TERMINATION_REQUEST || mscc.granted.total < 1) {
                    LOG.info("Terminate")
                    // Since this is a terminate reply no service is granted
                    gsuAvp.addAvp(Avp.CC_TIME, 0, true, false)
                    gsuAvp.addAvp(Avp.CC_TOTAL_OCTETS, 0L, true, false)
                    gsuAvp.addAvp(Avp.CC_SERVICE_SPECIFIC_UNITS, 0L, true, false)

                    addFinalUnitAction(answerMSCC, mscc)
                } else {
                    gsuAvp.addAvp(Avp.CC_TOTAL_OCTETS, mscc.granted.total, true, false)
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

        val origFinalUnitIndication: FinalUnitIndication? = mscc.finalUnitIndication
        if (origFinalUnitIndication != null) {
            val finalUnitIndication = answerMSCC.addGroupedAvp(Avp.FINAL_UNIT_INDICATION, true, false)
            finalUnitIndication.addAvp(Avp.FINAL_UNIT_ACTION, origFinalUnitIndication.finalUnitAction.ordinal, true, false)
        }

        //ToDo : Add support for the rest of the Final-Unit-Action
    }
}
