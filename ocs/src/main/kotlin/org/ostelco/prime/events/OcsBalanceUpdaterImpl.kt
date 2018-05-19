package org.ostelco.prime.events


import org.ostelco.prime.disruptor.PrimeEventProducer

class OcsBalanceUpdaterImpl(val producer: PrimeEventProducer) : OcsBalanceUpdater {

    override fun updateBalance(msisdn: String, noOfBytesToTopUp: Long) {
        // XXX removing '+' if it exists
        // XXX Use rewriting functions directly

        val sanitizedMsisdn: String = if (msisdn[0] == '+') {
            msisdn.substring(1)
        } else {
            msisdn
        }
        producer.topupDataBundleBalanceEvent(
                sanitizedMsisdn,
                noOfBytesToTopUp)
    }
}
