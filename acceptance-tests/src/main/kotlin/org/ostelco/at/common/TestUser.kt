package org.ostelco.at.common

import org.apache.commons.lang3.RandomStringUtils
import org.ostelco.at.jersey.post
import org.ostelco.prime.customer.model.Customer
import java.util.*

fun createCustomer(name: String, email: String): Customer {

    val createCustomer = Customer()
            .id("")
            .email(email)
            .name(name)
            .address("")
            .city("")
            .country("NO")
            .postCode("")
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

private val random = Random()
fun randomInt(): Int = random.nextInt(99999)