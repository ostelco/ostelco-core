package org.ostelco.at.common

import org.apache.commons.lang3.RandomStringUtils
import org.ostelco.at.jersey.post
import org.ostelco.prime.customer.model.Customer
import org.ostelco.prime.customer.model.ScanInformation
import java.util.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedHashMap

fun createCustomer(name: String, email: String): Customer {

    val createCustomer = Customer()
            .id("")
            .contactEmail(email)
            .nickname(name)
            .analyticsId("")
            .referralId("")

    return post {
        path = "/customer"
        body = createCustomer
        this.email = email
    }
}

fun createSubscription(email: String): String {

    val msisdn = RandomStringUtils.randomNumeric(8,9)

    post<String> {
        path = "/admin/subscriptions"
        queryParams = mapOf(
                "subscription_id" to email,
                "msisdn" to msisdn
        )
    }
    return msisdn
}

fun enableRegion(email: String) {

    val scanInformation = post<ScanInformation> {
        path = "/regions/no/kyc/jumio/scans"
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