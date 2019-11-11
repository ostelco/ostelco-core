package org.ostelco.prime.storage.graph

import arrow.core.Either
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.storage.StoreError

interface OnNewCustomerAction {
    fun apply(
            identity: Identity,
            customer: Customer,
            transaction: PrimeTransaction
    ): Either<StoreError, Unit>
}

interface AllowedRegionsService {
    fun get(customer: Customer,
            transaction: PrimeTransaction
    ): Either<StoreError, Collection<String>>
}

interface OnRegionApprovedAction {
    fun apply(
            customer: Customer,
            regionCode: String,
            transaction: PrimeTransaction
    ): Either<StoreError, Unit>
}

interface HssNameLookupService {
    fun getHssName(regionCode: String): String
}
