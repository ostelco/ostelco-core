package org.ostelco.tools.prime.admin.actions

import arrow.core.Either
import arrow.core.fix
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import arrow.effects.IO
import arrow.instances.either.monad.monad
import org.ostelco.prime.dsl.writeTransaction
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.Segment
import org.ostelco.prime.storage.NotFoundError
import org.ostelco.prime.storage.StoreError
import org.ostelco.prime.storage.ValidationError
import org.ostelco.prime.storage.graph.adminStore
import org.ostelco.prime.storage.graph.getSegmentNameFromCountryCode
import java.util.*

//
// Create
//

fun createCustomer(email: String, nickname: String): Either<StoreError, Unit> = adminStore
        .addCustomer(
                identity = Identity(
                        id = email,
                        type = "EMAIL",
                        provider = "email"
                ),
                customer = Customer(
                        id = UUID.randomUUID().toString(),
                        nickname = nickname,
                        contactEmail = email,
                        analyticsId = UUID.randomUUID().toString(),
                        referralId = UUID.randomUUID().toString()))

fun assignCustomerToRegionSegment(email: String, regionCode: String): Either<StoreError, Unit> = IO {
    Either.monad<StoreError>().binding {

        val customerId = adminStore.getCustomer(
                identity = Identity(
                        id = email,
                        type = "EMAIL",
                        provider = "email"
                )
        )
                .bind()
                .id

        adminStore.updateSegment(
                segment = Segment(
                        id = getSegmentNameFromCountryCode(regionCode),
                        subscribers = listOf(customerId)
                )
        )
                .bind()
    }.fix()
}.unsafeRunSync()

fun approveRegionForCustomer(email: String, regionCode: String): Either<StoreError, Unit> = IO {
    Either.monad<StoreError>().binding {

        val customerId = adminStore.getCustomer(
                identity = Identity(id = email, type = "EMAIL", provider = "email")
        )
                .bind()
                .id

        adminStore.updateSegment(
                segment = Segment(
                        id = getSegmentNameFromCountryCode(regionCode),
                        subscribers = listOf(customerId)
                )
        )
                .bind()

        adminStore.approveRegionForCustomer(
                customerId = customerId,
                regionCode = regionCode
        ).bind()

    }.fix()
}.unsafeRunSync()

fun deleteCustomer(email: String) = adminStore
        .removeCustomer(
                identity = Identity(
                        id = email,
                        type = "EMAIL",
                        provider = "email"
                )
        )

fun createSubscription(
        email: String,
        regionCode: String,
        iccId: String = "TEST-" + UUID.randomUUID().toString(),
        alias: String = "",
        msisdn: String) = adminStore
        .addSubscription(
                identity = Identity(
                        id = email,
                        type = "EMAIL",
                        provider = "email"
                ),
                regionCode = regionCode,
                iccId = iccId,
                alias = alias,
                msisdn = msisdn
        )

//
// Update
//

fun setBalance(email: String, balance: Long) = adminStore
        .getBundles(
                identity = Identity(
                        id = email,
                        type = "EMAIL",
                        provider = "email"
                )
        )
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
        .getAllRegionDetails(
                identity = Identity(
                        id = email,
                        type = "EMAIL",
                        provider = "email"
                )
        )

fun getRegionDetails(email: String, regionCode: String) = adminStore
        .getRegionDetails(
                identity = Identity(
                        id = email,
                        type = "EMAIL",
                        provider = "email"
                ),
                regionCode = regionCode
        )

