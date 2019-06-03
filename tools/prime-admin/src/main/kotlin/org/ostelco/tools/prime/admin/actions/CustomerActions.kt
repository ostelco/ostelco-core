package org.ostelco.tools.prime.admin.actions

import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import org.ostelco.prime.dsl.writeTransaction
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.storage.NotFoundError
import org.ostelco.prime.storage.ValidationError
import java.util.*

//
// Create
//

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

fun createSubscription(
        email: String,
        regionCode: String,
        iccId: String = UUID.randomUUID().toString(),
        alias: String = "",
        msisdn: String) {

    adminStore.addSubscription(
            identity = Identity(id = email, type = "EMAIL", provider = "email"),
            regionCode = regionCode,
            iccId = iccId,
            alias = alias,
            msisdn = msisdn)
}

//
// Update
//

fun setBalance(email: String, balance: Long) {

    adminStore.getBundles(identity = Identity(id = email, type = "EMAIL", provider = "email"))
            .flatMap { bundles ->
                when (bundles.size) {
                    0 -> NotFoundError(type = "Bundles", id = "email = $email").left()
                    1 -> bundles.single().id.right()
                    else -> ValidationError(type = "Bundles", id = "email = $email", message = "Found multiple bundles").left()
                }
            }
            .flatMap { bundleId ->
                writeTransaction {
                    update {
                        Bundle(id = bundleId, balance = 1_000_000_000L)
                    }
                }
            }
            .mapLeft {
                println(it.message)
            }
}

//
// Query
//

fun getAllRegionDetails(email: String) {

    adminStore.getAllRegionDetails(
            identity = Identity(id = email, type = "EMAIL", provider = "email"))
            .bimap({
                println(it.message)
            }, {
                println(formatJson(it))
            })
}

fun getRegionDetails(email: String, regionCode: String) {

    adminStore.getRegionDetails(
            identity = Identity(id = email, type = "EMAIL", provider = "email"),
            regionCode = regionCode)
            .bimap({
                println(it.message)
            }, {
                println(formatJson(it))
            })
}
