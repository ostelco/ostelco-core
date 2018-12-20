package org.ostelco.prime.ocs

import arrow.core.Either
import org.ostelco.prime.model.Bundle
import kotlin.DeprecationLevel.ERROR
import kotlin.DeprecationLevel.WARNING

@Deprecated(message = "This service bas been discontinued", level = WARNING)
interface OcsSubscriberService {
    @Deprecated(message = "This service bas been discontinued", level = ERROR)
    fun topup(subscriberId: String, sku: String): Either<String, Unit>
}

@Deprecated(message = "This service bas been discontinued", level = WARNING)
interface OcsAdminService {
    @Deprecated(message = "This service bas been discontinued", level = ERROR)
    fun addBundle(bundle: Bundle)
    @Deprecated(message = "This service bas been discontinued", level = ERROR)
    fun addMsisdnToBundleMapping(msisdn: String, bundleId: String)
}