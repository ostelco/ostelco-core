package org.ostelco.prime.ocs

import arrow.core.Either
import org.ostelco.prime.model.Bundle

interface OcsSubscriberService {
    fun topup(subscriberId: String, sku: String): Either<String, Unit>
}

interface OcsAdminService {
    fun addBundle(bundle: Bundle)
    fun addMsisdnToBundleMapping(msisdn: String, bundleId: String)
}