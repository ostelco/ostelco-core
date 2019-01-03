package org.ostelco.at.common

import org.apache.commons.lang3.RandomStringUtils
import org.ostelco.at.jersey.post
import org.ostelco.prime.client.model.Profile
import java.util.*

fun createProfile(name: String, email: String) {

    val createProfile = Profile()
            .email(email)
            .name(name)
            .address("")
            .city("")
            .country("NO")
            .postCode("")
            .referralId("")

    post<Profile> {
        path = "/profile"
        body = createProfile
        subscriberId = email
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