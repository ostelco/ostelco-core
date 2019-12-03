package org.ostelco.tools.prime.admin.actions

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import org.apache.commons.codec.digest.DigestUtils
import org.ostelco.prime.dsl.withId
import org.ostelco.prime.dsl.writeTransaction
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.storage.NotFoundError
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.ValidationError
import org.ostelco.prime.storage.graph.adminStore
import java.util.*

//
// Create
//

fun createCustomer(email: String, nickname: String): Either<StoreError, Unit> = adminStore
        .addCustomer(
                identity = emailAsIdentity(email = email),
                customer = Customer(
                        id = UUID.randomUUID().toString(),
                        nickname = nickname,
                        contactEmail = email,
                        analyticsId = UUID.randomUUID().toString(),
                        referralId = UUID.randomUUID().toString()))

fun approveRegionForCustomer(email: String, regionCode: String): Either<StoreError, Unit> = Either.fx {

    val customerId = adminStore
            .getCustomer(identity = emailAsIdentity(email = email))
            .bind()
            .id

    adminStore.approveRegionForCustomer(
            customerId = customerId,
            regionCode = regionCode
    ).bind()
}

fun addCustomerToSegment(email: String, segmentId: String): Either<StoreError, Unit> = writeTransaction {
    Either.fx {
        val customerId = adminStore
                .getCustomer(identity = emailAsIdentity(email = email))
                .bind()
                .id
        fact { (Customer withId customerId) belongsToSegment (org.ostelco.prime.storage.graph.model.Segment withId segmentId) }.bind()
    }
}

fun deleteCustomer(email: String) = adminStore
        .removeCustomer(identity = emailAsIdentity(email = email))

fun createSubscription(
        email: String,
        regionCode: String,
        iccId: String = "TEST-" + UUID.randomUUID().toString(),
        alias: String = "",
        msisdn: String) = adminStore
        .addSubscription(
                identity = emailAsIdentity(email = email),
                regionCode = regionCode,
                iccId = iccId,
                alias = alias,
                msisdn = msisdn
        )

//
// Update
//

fun setBalance(email: String, balance: Long) = adminStore
        .getBundles(identity = emailAsIdentity(email = email))
        .flatMap { bundles ->
            when (bundles.size) {
                0 -> NotFoundError(
                        type = "Bundles",
                        id = "email = $email"
                ).left()
                1 -> bundles.single().id.right()
                else -> ValidationError(
                        type = "Bundles",
                        id = "email = $email",
                        message = "Found multiple bundles"
                ).left()
            }
        }
        .flatMap { bundleId ->
            writeTransaction {
                update {
                    Bundle(id = bundleId, balance = balance)
                }
            }
        }
        .mapLeft {
            println(it.message)
        }

//
// Query
//

fun getAllRegionDetails(email: String) = adminStore
        .getAllRegionDetails(identity = emailAsIdentity(email = email))

fun getRegionDetails(email: String, regionCode: String) = adminStore
        .getRegionDetails(
                identity = emailAsIdentity(email = email),
                regionCode = regionCode
        )

// Common
private fun emailAsIdentity(email: String) = Identity(
        id = email,
        type = "EMAIL",
        provider = "email"
)

fun identifyCustomer(idDigests: Set<String>) {
    Either.fx<StoreError, Unit> {
        adminStore
                // get all Identity values
                .getAllIdentities()
                .bind()
                // map to <Digest of id, Identity>
                .map { String(Base64.getEncoder().encode(DigestUtils.sha256(it.id))) to it }
                .toMap()
                // filter on idDigests we want to find
                .filterKeys { idDigests.contains(it) }
                // find customer for each filtered Identity
                .forEach { (idDigest, identity) ->
                    adminStore.getCustomer(identity).bimap(
                            { println("No Customer found for digest: $idDigest. - ${it.message}") },
                            { println("Found Customer ID: ${it.id} for digest: $idDigest") }
                    )
                }
    }.mapLeft {
        println(it.message)
    }
}