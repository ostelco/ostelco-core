package org.ostelco.prime.ocs.core

import org.ostelco.prime.getLogger

object Rating {
    enum class Rate {
        BLOCKED, NORMAL, ZERO
    }

    private val logger by getLogger()
    private val hashMap: HashMap<RateIdentifier, Rate> = HashMap<RateIdentifier,Rate>()

    /**
     *  This is an early version of rating with hardcoded values.
     *  The rate should be descided on the subscription, location, Service-Identifier
     *  and the Rating-Group for the traffic.
     */
    @Suppress("UNUSED_PARAMETER")
    fun getRate(msisdn: String, serviceIdentifier: Long, ratingGroup: Long, mccmnc: String) : Rate {
        return hashMap.get(RateIdentifier(serviceIdentifier, ratingGroup)) ?: Rating.Rate.BLOCKED
    }

    fun addRate(serviceIdentifier: Long, ratingGroup: Long, rate: String) {
        logger.info("Adding rate for {} {} : {}", serviceIdentifier, ratingGroup, rate)
        hashMap.put(RateIdentifier(serviceIdentifier, ratingGroup), Rating.Rate.valueOf(rate.toUpperCase()))
    }

    data class RateIdentifier(
            val serviceId: Long,
            val ratingGroup: Long)
}