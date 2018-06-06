package org.ostelco.prime.ocs

interface OcsSubscriberService {
    fun topup(msisdn: String, sku: String)
}
