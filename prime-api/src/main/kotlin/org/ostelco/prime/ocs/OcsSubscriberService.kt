package org.ostelco.prime.ocs

interface OcsSubscriberService {
    fun getBalance(msisdn: String) : Long
    fun topup(msisdn: String, sku: String)
}
