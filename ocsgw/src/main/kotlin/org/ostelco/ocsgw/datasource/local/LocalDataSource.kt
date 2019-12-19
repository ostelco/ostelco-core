package org.ostelco.ocsgw.datasource.local

import org.jdiameter.api.IllegalDiameterStateException
import org.jdiameter.api.InternalException
import org.jdiameter.api.OverloadException
import org.jdiameter.api.RouteException
import org.jdiameter.api.cca.ServerCCASession
import org.ostelco.diameter.CreditControlContext
import org.ostelco.diameter.model.*
import org.ostelco.ocs.api.CreditControlRequestType
import OcsServer.stack
import org.ostelco.diameter.getLogger
import org.ostelco.ocsgw.datasource.DataSource
import java.util.*

/**
 * Local DataSource that will accept all Credit Control Requests
 * Can be used as a bypass.
 */
class LocalDataSource : DataSource {

    private val logger by getLogger()

    override fun init() { // No init needed
    }

    override fun handleRequest(context: CreditControlContext) {
        val answer = createCreditControlAnswer(context)
        logger.info("Got Credit-Control-Request [{}] [{}]", context.creditControlRequest.msisdn, context.sessionId)
        try {
            val session = stack!!.getSession(context.sessionId, ServerCCASession::class.java)
            session.sendCreditControlAnswer(context.createCCA(answer))
            logger.info("Sent Credit-Control-Answer [{}] [{}]", context.creditControlRequest.msisdn, context.sessionId)
        } catch (e: InternalException) {
            logger.error("Failed to send Credit-Control-Answer. [{}] [{}]", context.creditControlRequest.msisdn, context.sessionId, e)
        } catch (e: IllegalDiameterStateException) {
            logger.error("Failed to send Credit-Control-Answer. [{}] [{}]", context.creditControlRequest.msisdn, context.sessionId, e)
        } catch (e: RouteException) {
            logger.error("Failed to send Credit-Control-Answer. [{}] [{}]", context.creditControlRequest.msisdn, context.sessionId, e)
        } catch (e: OverloadException) {
            logger.error("Failed to send Credit-Control-Answer. [{}] [{}]", context.creditControlRequest.msisdn, context.sessionId, e)
        } catch (e: NullPointerException) {
            logger.error("Failed to send Credit-Control-Answer. [{}] [{}]", context.creditControlRequest.msisdn, context.sessionId, e)
        }
    }

    override fun isBlocked(msisdn: String): Boolean {
        return false
    }

    private fun createCreditControlAnswer(context: CreditControlContext): CreditControlAnswer {
        val origMultipleServiceCreditControls = context.creditControlRequest.multipleServiceCreditControls
        val newMultipleServiceCreditControls: MutableList<MultipleServiceCreditControl> = ArrayList()
        for (mscc in origMultipleServiceCreditControls) {
            var finalUnitIndication: FinalUnitIndication? = null
            if (context.originalCreditControlRequest.requestTypeAVPValue == CreditControlRequestType.TERMINATION_REQUEST.number) {
                finalUnitIndication = FinalUnitIndication(
                        FinalUnitAction.TERMINATE,
                        ArrayList(),
                        ArrayList(),
                        RedirectServer(RedirectAddressType.IPV4_ADDRESS,"")
                )
            }
            val newRequested: MutableList<ServiceUnit> = ArrayList()
            for (requested in mscc.requested) {
                newRequested.add(ServiceUnit(requested.total, 0, 0))
            }
            if (!newRequested.isEmpty()) {
                val granted = newRequested[0]
                val newMscc = MultipleServiceCreditControl(
                        mscc.ratingGroup,
                        mscc.serviceIdentifier,
                        newRequested,
                        mscc.used,
                        granted,
                        mscc.validityTime,
                        7200,
                        (granted.total * 0.2).toLong(),  // 20%
                        finalUnitIndication,
                        ResultCode.DIAMETER_SUCCESS)
                newMultipleServiceCreditControls.add(newMscc)
            }
        }
        var validityTime = 0
        if (newMultipleServiceCreditControls.isEmpty()) {
            validityTime = 86400
        }
        return CreditControlAnswer(ResultCode.DIAMETER_SUCCESS, newMultipleServiceCreditControls, validityTime)
    }
}