package org.ostelco.at.common

import org.apache.commons.lang3.RandomStringUtils
import org.ostelco.at.jersey.get
import org.ostelco.at.jersey.post
import org.ostelco.at.jersey.put
import org.ostelco.prime.customer.model.Customer
import org.ostelco.prime.customer.model.ScanInformation
import java.util.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedHashMap

fun createCustomer(name: String, email: String): Customer {

    return post {
        path = "/customer"
        queryParams = mapOf("nickname" to name, "contactEmail" to email)
        this.email = email
    }
}

fun createSubscription(email: String): String {

    val msisdn = RandomStringUtils.randomNumeric(8,9)

    post<String> {
        path = "/admin/subscriptions"
        queryParams = mapOf(
                "email" to email,
                "regionCode" to "no",
                "iccId" to "TEST-unknown",
                "alias" to "default",
                "msisdn" to msisdn
        )
    }
    return msisdn
}

fun enableRegion(email: String, region: String = "no") {

    when (region) {
        "sg" -> {
            get<String> {
                path = "/regions/sg/kyc/myInfo/v3/personData/activation-code"
                this.email = email
            }
            put<String>(expectedResultCode = 204) {
                path = "/regions/sg/kyc/profile"
                this.email = email
                queryParams = mapOf("address" to "Singapore")
            }
        }
        else -> performJumioKyc(email = email, region = region)
    }
}

private fun performJumioKyc(email: String, region: String) {

    val scanInformation = post<ScanInformation> {
        path = "/regions/$region/kyc/jumio/scans"
        this.email = email
    }

    post<String>(expectedResultCode = 200, dataType = MediaType.APPLICATION_FORM_URLENCODED_TYPE) {
        path = "/ekyc/callback"
        body = MultivaluedHashMap(mapOf(
                "jumioIdScanReference" to UUID.randomUUID().toString(),
                "idScanStatus" to "SUCCESS",
                "verificationStatus" to "APPROVED_VERIFIED",
                "callbackDate" to "2018-12-07T09:19:07.036Z",
                "idCountry" to "NOR",
                "merchantIdScanReference" to scanInformation.scanId,
                "identityVerification" to """{ "similarity":"MATCH", "validity":"TRUE"}"""))
    }
}

private val random = Random()
fun randomInt(): Int = random.nextInt(99999)