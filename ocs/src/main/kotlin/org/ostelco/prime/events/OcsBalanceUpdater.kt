package org.ostelco.prime.events

interface OcsBalanceUpdater {
    fun updateBalance(msisdn: String, noOfBytesToTopUp: Long)
}
