package org.ostelco.prime.storage.graph

import arrow.core.Either
import org.ostelco.prime.model.Identity
import org.ostelco.prime.storage.StoreError

interface HssNameLookupService {
    fun getHssName(regionCode: String): String
}

interface OnNewCustomerAction {
    fun apply(identity: Identity,
              customerId: String,
              transaction: PrimeTransaction): Either<StoreError, Unit>
}