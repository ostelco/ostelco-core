package org.ostelco.diameter.test

import org.jdiameter.api.Avp
import org.jdiameter.api.Avp.PS_INFORMATION
import org.jdiameter.api.AvpSet
import org.ostelco.diameter.builder.set
import org.ostelco.diameter.model.RequestType
import org.ostelco.diameter.model.SubscriptionType

/**
 * Helper class to create the most common AVP combinations
 * for a Diameter Credit-Control Application.
 */
object TestHelper {

    private const val VENDOR_ID_3GPP = 10415L
    private const val IMSI = "242017100000228"
    private const val APN = "panacea"
    private const val SGSN_MCC_MNC = "24201"
    private const val CALLED_STATION_ID = 30

    private fun buildBasicRequest(ccrAvps: AvpSet, requestType: Int, requestNumber: Int) {

        set(ccrAvps) {
            avp(Avp.CC_REQUEST_TYPE, requestType)
            avp(Avp.CC_REQUEST_NUMBER, requestNumber)
        }
    }

    private fun addUser(ccrAvps: AvpSet, msisdn: String, imsi: String) {

        set(ccrAvps) {
            group(Avp.SUBSCRIPTION_ID) {
                avp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164.ordinal)
                avp(Avp.SUBSCRIPTION_ID_DATA, msisdn, pFlag = true)
            }
            group(Avp.SUBSCRIPTION_ID) {
                avp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_IMSI.ordinal)
                avp(Avp.SUBSCRIPTION_ID_DATA, imsi, pFlag = true)
            }
        }
    }

    private fun addBucketRequest(ccrAvps: AvpSet, ratingGroup: Int, serviceIdentifier: Int, bucketSize: Long) {

        set(ccrAvps) {

            avp(Avp.MULTIPLE_SERVICES_INDICATOR, 1, pFlag = true)

            group(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL) {
                avp(Avp.RATING_GROUP, ratingGroup, pFlag = true)
                avp(Avp.SERVICE_IDENTIFIER_CCA, serviceIdentifier, pFlag = true)

                group(Avp.REQUESTED_SERVICE_UNIT) {
                    avp(Avp.CC_TOTAL_OCTETS, bucketSize, pFlag = true)
                    avp(Avp.CC_INPUT_OCTETS, 0L, pFlag = true)
                    avp(Avp.CC_OUTPUT_OCTETS, 0L, pFlag = true)
                }
            }
        }
    }

    private fun addFinalBucketRequest(ccrAvps: AvpSet, ratingGroup: Int, serviceIdentifier: Int) {

        set(ccrAvps) {

            avp(Avp.MULTIPLE_SERVICES_INDICATOR, 1, pFlag = true)

            group(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL) {
                group(Avp.USED_SERVICE_UNIT) {
                    avp(Avp.CC_TIME, 0, pFlag = true)
                    avp(Avp.CC_SERVICE_SPECIFIC_UNITS, 0L, pFlag = true)
                }
                avp(Avp.RATING_GROUP, ratingGroup, pFlag = true)
                avp(Avp.SERVICE_IDENTIFIER_CCA, serviceIdentifier, pFlag = true)
                avp(Avp.REPORTING_REASON, 2, VENDOR_ID_3GPP, pFlag = true)
            }
        }
    }

    private fun addTerminateRequest(ccrAvps: AvpSet, ratingGroup: Int, serviceIdentifier: Int, bucketSize: Long) {

        set(ccrAvps) {

            avp(Avp.TERMINATION_CAUSE, 1, pFlag = true) // 1 = DIAMETER_LOGOUT

            group(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL) {
                avp(Avp.RATING_GROUP, ratingGroup, pFlag = true)
                avp(Avp.SERVICE_IDENTIFIER_CCA, serviceIdentifier, pFlag = true)

                group(Avp.USED_SERVICE_UNIT) {
                    avp(Avp.CC_TOTAL_OCTETS, bucketSize, pFlag = true)
                    avp(Avp.CC_INPUT_OCTETS, 0L, pFlag = true)
                    avp(Avp.CC_OUTPUT_OCTETS, 0L, pFlag = true)
                    avp(Avp.CC_SERVICE_SPECIFIC_UNITS, 0L, pFlag = true)
                }
                avp(Avp.REPORTING_REASON, 2, vendorId = VENDOR_ID_3GPP) // 2 = FINAL
            }
        }
    }

    private fun addServiceInformation(ccrAvps: AvpSet, apn: String, sgsnMncMcc: String) {

        set(ccrAvps) {

            group(Avp.SERVICE_INFORMATION, vendorId = VENDOR_ID_3GPP) {
                group(PS_INFORMATION, vendorId = VENDOR_ID_3GPP) {
                    avp(CALLED_STATION_ID, apn, pFlag = true)
                    avp(Avp.GPP_SGSN_MCC_MNC, sgsnMncMcc, vendorId = VENDOR_ID_3GPP, asOctetString = true)
                }
            }
        }
    }



    @JvmStatic
    fun createInitRequest(ccrAvps: AvpSet, msisdn: String, bucketSize: Long) {
        buildBasicRequest(ccrAvps, RequestType.INITIAL_REQUEST, requestNumber = 0)
        addUser(ccrAvps, msisdn = msisdn, imsi = IMSI)
        addBucketRequest(ccrAvps, ratingGroup = 10, serviceIdentifier = 1, bucketSize = bucketSize)
        addServiceInformation(ccrAvps, apn = APN, sgsnMncMcc = SGSN_MCC_MNC)
    }

    @JvmStatic
    fun createUpdateRequest(ccrAvps: AvpSet, msisdn: String, bucketSize: Long) {
        buildBasicRequest(ccrAvps, RequestType.UPDATE_REQUEST, requestNumber = 1)
        addUser(ccrAvps, msisdn = msisdn, imsi = IMSI)
        addBucketRequest(ccrAvps, ratingGroup = 10, serviceIdentifier = 1, bucketSize = bucketSize)
        addServiceInformation(ccrAvps, apn = APN, sgsnMncMcc = SGSN_MCC_MNC)
    }

    @JvmStatic
    fun createUpdateRequestFinal(ccrAvps: AvpSet, msisdn: String) {
        buildBasicRequest(ccrAvps, RequestType.UPDATE_REQUEST, requestNumber = 1)
        addUser(ccrAvps, msisdn = msisdn, imsi = IMSI)
        addFinalBucketRequest(ccrAvps, ratingGroup = 10, serviceIdentifier = 1)
        addServiceInformation(ccrAvps, apn = APN, sgsnMncMcc = SGSN_MCC_MNC)
    }

    @JvmStatic
    fun createTerminateRequest(ccrAvps: AvpSet, msisdn: String, bucketSize: Long) {
        buildBasicRequest(ccrAvps, RequestType.TERMINATION_REQUEST, requestNumber = 2)
        addUser(ccrAvps, msisdn = msisdn, imsi = IMSI)
        addTerminateRequest(ccrAvps, ratingGroup = 10, serviceIdentifier = 1, bucketSize = bucketSize)
        addServiceInformation(ccrAvps, apn = APN, sgsnMncMcc = SGSN_MCC_MNC)
    }
}