package org.ostelco.diameter.test

import org.jdiameter.api.Avp
import org.jdiameter.api.AvpSet
import org.ostelco.diameter.model.RequestType
import org.ostelco.diameter.model.SubscriptionType

class TestHelper {

    companion object {
        private const val VENDOR_ID_3GPP = 10415L
        private const val MSISDN = "4747900184"
        private const val IMSI = "242017100000228"
        private const val APN = "panacea"
        private const val SGSN_MCC_MNC = "24201"
        private const val CALLED_STATION_ID = 30
        private const val BUCKET_SIZE = 500L
    }

    private fun init(ccrAvps: AvpSet) {

        ccrAvps.addAvp(Avp.CC_REQUEST_TYPE, RequestType.INITIAL_REQUEST, true, false)
        ccrAvps.addAvp(Avp.CC_REQUEST_NUMBER, 0, true, false)

        val subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164.ordinal)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, MSISDN, false)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_IMSI.ordinal)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, IMSI, false)

        ccrAvps.addAvp(Avp.MULTIPLE_SERVICES_INDICATOR, 1)

        val mscc = ccrAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
        mscc.addAvp(Avp.RATING_GROUP, 10)
        mscc.addAvp(Avp.SERVICE_IDENTIFIER_CCA, 1)
        val requestedServiceUnits = mscc.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT)
        requestedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, BUCKET_SIZE)
        requestedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L)
        requestedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L)

        val serviceInformation = ccrAvps.addGroupedAvp(Avp.SERVICE_INFORMATION, VENDOR_ID_3GPP, true, false)
        val psInformation = serviceInformation.addGroupedAvp(Avp.PS_INFORMATION, VENDOR_ID_3GPP, true, false)
        psInformation.addAvp(CALLED_STATION_ID, APN, false)
        psInformation.addAvp(Avp.GPP_SGSN_MCC_MNC, SGSN_MCC_MNC, VENDOR_ID_3GPP, true, false, true)
    }

    private fun update(ccrAvps: AvpSet) {

        ccrAvps.addAvp(Avp.CC_REQUEST_TYPE, RequestType.UPDATE_REQUEST, true, false)
        ccrAvps.addAvp(Avp.CC_REQUEST_NUMBER, 1, true, false)

        val subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164.ordinal)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, MSISDN, false)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_IMSI.ordinal)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, IMSI, false)

        ccrAvps.addAvp(Avp.MULTIPLE_SERVICES_INDICATOR, 1)

        val mscc = ccrAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
        mscc.addAvp(Avp.RATING_GROUP, 10)
        mscc.addAvp(Avp.SERVICE_IDENTIFIER_CCA, 1)
        val requestedServiceUnits = mscc.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT)
        requestedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, BUCKET_SIZE)
        requestedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L)
        requestedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L)

        val serviceInformation = ccrAvps.addGroupedAvp(Avp.SERVICE_INFORMATION, VENDOR_ID_3GPP, true, false)
        val psInformation = serviceInformation.addGroupedAvp(Avp.PS_INFORMATION, VENDOR_ID_3GPP, true, false)
        psInformation.addAvp(CALLED_STATION_ID, APN, false)
        psInformation.addAvp(Avp.GPP_SGSN_MCC_MNC, SGSN_MCC_MNC, VENDOR_ID_3GPP, true, false, true)
    }

    fun terminate(ccrAvps: AvpSet) {

        ccrAvps.addAvp(Avp.CC_REQUEST_TYPE, RequestType.TERMINATION_REQUEST, true, false)
        ccrAvps.addAvp(Avp.CC_REQUEST_NUMBER, 2, true, false)

        val subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164.ordinal)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, MSISDN, false)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_IMSI.ordinal)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, IMSI, false)

        ccrAvps.addAvp(Avp.TERMINATION_CAUSE, 1, true, false) // 1 = DIAMETER_LOGOUT

        val mscc = ccrAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
        mscc.addAvp(Avp.RATING_GROUP, 10)
        mscc.addAvp(Avp.SERVICE_IDENTIFIER_CCA, 1)
        val usedServiceUnits = mscc.addGroupedAvp(Avp.USED_SERVICE_UNIT)
        usedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, BUCKET_SIZE)
        usedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L)
        usedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L)
        usedServiceUnits.addAvp(Avp.CC_SERVICE_SPECIFIC_UNITS, 0L)
        mscc.addAvp(Avp.REPORTING_REASON, 2, VENDOR_ID_3GPP, true, false) // 2 = FINAL

        val serviceInformation = ccrAvps.addGroupedAvp(Avp.SERVICE_INFORMATION, VENDOR_ID_3GPP, true, false)
        val psInformation = serviceInformation.addGroupedAvp(Avp.PS_INFORMATION, VENDOR_ID_3GPP, true, false)
        psInformation.addAvp(CALLED_STATION_ID, APN, false)
        psInformation.addAvp(Avp.GPP_SGSN_MCC_MNC, SGSN_MCC_MNC, VENDOR_ID_3GPP, true, false, true)
    }

    fun initNoCredit(ccrAvps: AvpSet) {

        ccrAvps.addAvp(Avp.CC_REQUEST_TYPE, RequestType.INITIAL_REQUEST, true, false)
        ccrAvps.addAvp(Avp.CC_REQUEST_NUMBER, 0, true, false)

        val subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164.ordinal)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, "4333333333", false)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_IMSI.ordinal)
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, IMSI, false)

        ccrAvps.addAvp(Avp.MULTIPLE_SERVICES_INDICATOR, 1)

        val mscc = ccrAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
        mscc.addAvp(Avp.RATING_GROUP, 10)
        mscc.addAvp(Avp.SERVICE_IDENTIFIER_CCA, 1)
        val requestedServiceUnits = mscc.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT)
        requestedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, BUCKET_SIZE)
        requestedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L)
        requestedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L)

        val serviceInformation = ccrAvps.addGroupedAvp(Avp.SERVICE_INFORMATION, VENDOR_ID_3GPP, true, false)
        val psInformation = serviceInformation.addGroupedAvp(Avp.PS_INFORMATION, VENDOR_ID_3GPP, true, false)
        psInformation.addAvp(CALLED_STATION_ID, APN, false)
        psInformation.addAvp(Avp.GPP_SGSN_MCC_MNC, SGSN_MCC_MNC, VENDOR_ID_3GPP, true, false, true)
    }
}