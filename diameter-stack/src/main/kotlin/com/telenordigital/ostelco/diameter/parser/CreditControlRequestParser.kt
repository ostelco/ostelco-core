package com.telenordigital.ostelco.diameter.parser

import com.telenordigital.ostelco.diameter.logger
import com.telenordigital.ostelco.diameter.model.CreditControlRequest
import com.telenordigital.ostelco.diameter.model.MultipleServiceCreditControl
import com.telenordigital.ostelco.diameter.model.ServiceInformation
import com.telenordigital.ostelco.diameter.model.SubscriptionType
import com.telenordigital.ostelco.diameter.model.SubscriptionType.END_USER_E164
import com.telenordigital.ostelco.diameter.model.SubscriptionType.END_USER_IMSI
import com.telenordigital.ostelco.diameter.model.SubscriptionType.END_USER_NAI
import com.telenordigital.ostelco.diameter.model.SubscriptionType.END_USER_PRIVATE
import com.telenordigital.ostelco.diameter.model.SubscriptionType.END_USER_SIP_URI
import com.telenordigital.ostelco.diameter.model.UserEquipmentInfo
import com.telenordigital.ostelco.diameter.util.DiameterUtilities
import org.jdiameter.api.Avp
import org.jdiameter.api.AvpDataException
import org.jdiameter.api.AvpSet
import org.jdiameter.api.InternalException
import org.jdiameter.api.cca.events.JCreditControlRequest
import java.util.*

class CreditControlRequestParser(val request: JCreditControlRequest) : Parser<CreditControlRequest> {

    private val LOG by logger()

    private var ccrAvps: AvpSet? = null
    private val multipleServiceCreditControls = LinkedList<MultipleServiceCreditControl>()
    private var serviceInformation: ServiceInformation? = null
    private var userEquipmentInfo: UserEquipmentInfo? = null
    private var msisdn = ""
    private var imsi = ""
    private var ccRequestType: Avp? = null
    private var ccRequestNumber: Avp? = null

    init {
        parseRequest(request)
    }

    override fun parse(): CreditControlRequest {
        return CreditControlRequest(
                ccrAvps,
                multipleServiceCreditControls,
                serviceInformation,
                userEquipmentInfo,
                msisdn,
                imsi,
                ccRequestType,
                ccRequestNumber)
    }

    private fun parseRequest(request: JCreditControlRequest) {
        try {
            val ccrAvps: AvpSet? = request.message.avps
            this.ccrAvps = ccrAvps
            LOG.info("Credit-Control-Request")
            DiameterUtilities().printAvps(ccrAvps) // TODO check for null
            if (ccrAvps != null) {
                ccRequestType = ccrAvps.getAvp(Avp.CC_REQUEST_TYPE)
                ccRequestNumber = ccrAvps.getAvp(Avp.CC_REQUEST_NUMBER)
                parseMultipleServiceCreditControl(ccrAvps)
                parseSubscriptionId(ccrAvps)
                parseServiceInformation(ccrAvps)
                parseUserEquipmentInfo(ccrAvps)
            }
        } catch (e: InternalException) {
            LOG.error("Failed to parse CCR", e)
        }
    }

    // User-Equipment-Info (AVP 458)
    private fun parseUserEquipmentInfo(ccrAvps: AvpSet) {
        try {
            val set = ccrAvps.getAvp(Avp.USER_EQUIPMENT_INFO)?.grouped
            if (set != null) {
                userEquipmentInfo = UserEquipmentInfoParser(set).parse()
            }
        } catch (e: AvpDataException) {
            LOG.error("Failed to parse User-Equipment-Info", e)
        }
    }

    // Multiple-Service-Credit-Control (AVP 456)
    private fun parseMultipleServiceCreditControl(ccrAvps: AvpSet) {
        try {
            val requestMsccSet = ccrAvps.getAvps(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
            for (msccAvp in requestMsccSet) {
                val serviceControl = msccAvp.grouped
                val mscc = MultipleServiceCreditControlParser(serviceControl).parse()
                multipleServiceCreditControls.add(mscc)
            }
        } catch (e: AvpDataException) {
            LOG.error("parseMultipleServiceCreditControl failed ", e)
        }

    }

    // Subscription-Id (AVP 443)
    private fun parseSubscriptionId(ccrAvps: AvpSet) {

        try {
            val subscriptionAvps = ccrAvps.getAvps(Avp.SUBSCRIPTION_ID)

            for (sidP in subscriptionAvps) {
                val sid = sidP.grouped
                val subscriptionType = sid.getAvp(Avp.SUBSCRIPTION_ID_TYPE).integer32

                when (SubscriptionType.values()[subscriptionType]) {
                    END_USER_E164 -> msisdn = sid.getAvp(Avp.SUBSCRIPTION_ID_DATA).utF8String
                    END_USER_IMSI -> imsi = sid.getAvp(Avp.SUBSCRIPTION_ID_DATA).utF8String
                    END_USER_SIP_URI,
                    END_USER_NAI,
                    END_USER_PRIVATE -> {
                    }
                }
            }
        } catch (e: AvpDataException) {
            LOG.error("parseSubscriptionId failed", e)
        }
    }

    // Service-Information (AVP 873)
    private fun parseServiceInformation(ccrAvps: AvpSet) {
        try {
            val serviceInformationAvp: Avp? = ccrAvps.getAvp(Avp.SERVICE_INFORMATION)
            if (serviceInformationAvp != null) {
                serviceInformation = ServiceInformationParser(serviceInformationAvp).parse()
            } else {
                LOG.info("No Service-Information")
            }
        } catch (e: NullPointerException) {
            LOG.error("Failed to parse ServiceInformation", e)
        }

    }
}
