package org.ostelco.diameter.test

import org.jdiameter.api.Avp
import org.jdiameter.api.Avp.PS_INFORMATION
import org.jdiameter.api.AvpSet
import org.ostelco.diameter.builder.set
import org.ostelco.diameter.model.RequestType
import org.ostelco.diameter.model.SubscriptionType

object TestHelper {

    private const val VENDOR_ID_3GPP = 10415L
    private const val IMSI = "242017100000228"
    private const val APN = "panacea"
    private const val SGSN_MCC_MNC = "24201"
    private const val CALLED_STATION_ID = 30

    private fun build(ccrAvps: AvpSet, requestType: Int, requestNumber: Int, msisdn: String, bucketSize: Long) {

        set(ccrAvps) {
            avp(Avp.CC_REQUEST_TYPE, requestType)
            avp(Avp.CC_REQUEST_NUMBER, requestNumber)

            group(Avp.SUBSCRIPTION_ID) {
                avp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164.ordinal)
                avp(Avp.SUBSCRIPTION_ID_DATA, msisdn, pFlag = true)
            }
            group(Avp.SUBSCRIPTION_ID) {
                avp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_IMSI.ordinal)
                avp(Avp.SUBSCRIPTION_ID_DATA, IMSI, pFlag = true)
            }

            avp(Avp.MULTIPLE_SERVICES_INDICATOR, 1, pFlag = true)

            group(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL) {
                avp(Avp.RATING_GROUP, 10, pFlag = true)
                avp(Avp.SERVICE_IDENTIFIER_CCA, 1, pFlag = true)

                group(Avp.REQUESTED_SERVICE_UNIT) {
                    avp(Avp.CC_TOTAL_OCTETS, bucketSize, pFlag = true)
                    avp(Avp.CC_INPUT_OCTETS, 0L, pFlag = true)
                    avp(Avp.CC_OUTPUT_OCTETS, 0L, pFlag = true)
                }
            }

            group(Avp.SERVICE_INFORMATION, vendorId = VENDOR_ID_3GPP) {
                group(PS_INFORMATION, vendorId = VENDOR_ID_3GPP) {
                    avp(CALLED_STATION_ID, APN, pFlag = true)
                    avp(Avp.GPP_SGSN_MCC_MNC, SGSN_MCC_MNC, vendorId = VENDOR_ID_3GPP, asOctetString = true)
                }
            }
        }
    }

    fun init(ccrAvps: AvpSet, msisdn: String, bucketSize: Long) {
        build(ccrAvps, RequestType.INITIAL_REQUEST, requestNumber = 0, msisdn = msisdn, bucketSize = bucketSize)
    }

    fun initNoCredit(ccrAvps: AvpSet, bucketSize: Long) {
        build(ccrAvps, RequestType.INITIAL_REQUEST, requestNumber = 0, msisdn = "4333333333", bucketSize = bucketSize)
    }

    fun update(ccrAvps: AvpSet, msisdn: String, bucketSize: Long) {
        build(ccrAvps, RequestType.UPDATE_REQUEST, requestNumber = 1, msisdn = msisdn, bucketSize = bucketSize)
    }

    fun terminate(ccrAvps: AvpSet, msisdn: String, bucketSize: Long) {

        set(ccrAvps) {
            avp(Avp.CC_REQUEST_TYPE, RequestType.TERMINATION_REQUEST)
            avp(Avp.CC_REQUEST_NUMBER, 2)

            group(Avp.SUBSCRIPTION_ID) {
                avp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164.ordinal)
                avp(Avp.SUBSCRIPTION_ID_DATA, msisdn, pFlag = true)
            }
            group(Avp.SUBSCRIPTION_ID) {
                avp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_IMSI.ordinal)
                avp(Avp.SUBSCRIPTION_ID_DATA, IMSI, pFlag = true)
            }

            avp(Avp.TERMINATION_CAUSE, 1, pFlag = true) // 1 = DIAMETER_LOGOUT

            group(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL) {
                avp(Avp.RATING_GROUP, 10, pFlag = true)
                avp(Avp.SERVICE_IDENTIFIER_CCA, 1, pFlag = true)

                group(Avp.REQUESTED_SERVICE_UNIT) {
                    avp(Avp.CC_TOTAL_OCTETS, bucketSize, pFlag = true)
                    avp(Avp.CC_INPUT_OCTETS, 0L, pFlag = true)
                    avp(Avp.CC_OUTPUT_OCTETS, 0L, pFlag = true)
                    avp(Avp.CC_SERVICE_SPECIFIC_UNITS, 0L, pFlag = true)
                }
                avp(Avp.REPORTING_REASON, 2, vendorId = VENDOR_ID_3GPP) // 2 = FINAL
            }

            group(Avp.SERVICE_INFORMATION, vendorId = VENDOR_ID_3GPP) {
                group(PS_INFORMATION, vendorId = VENDOR_ID_3GPP) {
                    avp(CALLED_STATION_ID, APN, pFlag = true)
                    avp(Avp.GPP_SGSN_MCC_MNC, SGSN_MCC_MNC, vendorId = VENDOR_ID_3GPP, asOctetString = true)
                }
            }
        }
    }
}