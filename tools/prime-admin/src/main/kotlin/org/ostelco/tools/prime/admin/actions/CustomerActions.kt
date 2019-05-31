package org.ostelco.tools.prime.admin.actions

import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import java.util.*

fun createCustomer(email: String, nickname: String) {

    adminStore.addCustomer(
            identity = Identity(id = email, type = "EMAIL", provider = "email"),
            customer = Customer(
                    id = UUID.randomUUID().toString(),
                    nickname = nickname,
                    contactEmail = email,
                    analyticsId = UUID.randomUUID().toString(),
                    referralId = UUID.randomUUID().toString()))
            .mapLeft {
                println(it.message)
            }
}

fun getAllRegionDetails(email: String) {

    adminStore.getAllRegionDetails(
            identity = Identity(id = email, type = "EMAIL", provider = "email"))
            .bimap({
                println(it.message)
            }, {
                println(formatJson(it))
            })
}
