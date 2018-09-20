package org.ostelco.prime.ocs

import org.ostelco.prime.model.Bundle

interface OcsSubscriberService {
    fun topup(subscriberId: String, sku: String)
}

interface OcsAdminService {
    fun addBundle(bundle: Bundle)
    fun addMsisdnToBundleMapping(msisdn: String, bundleId: String)
}