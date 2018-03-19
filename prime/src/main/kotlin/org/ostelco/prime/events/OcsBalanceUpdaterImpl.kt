package org.ostelco.prime.events


import com.google.common.base.Preconditions.checkNotNull
import org.ostelco.prime.disruptor.PrimeEventProducer

class OcsBalanceUpdaterImpl(producer: PrimeEventProducer) : OcsBalanceUpdater {
    private val producer: PrimeEventProducer

    init {
        this.producer = checkNotNull(producer)
    }


    override fun updateBalance(msisdn: String, noOfBytesToTopUp: Long) {
        // XXX removing '+' if it exists
        // XXX Use rewriting functions directly

        val sanitizedMsisdn: String
        if (msisdn[0] == '+') {
            sanitizedMsisdn = msisdn.substring(1)
        } else {
            sanitizedMsisdn = msisdn
        }
        producer.topupDataBundleBalanceEvent(
                sanitizedMsisdn,
                noOfBytesToTopUp)
    }
}
