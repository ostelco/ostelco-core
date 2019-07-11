package org.ostelco.prime.ocs.core

import org.ostelco.prime.getLogger

object Rating {
    enum class Rate {
        BLOCKED, NORMAL, ZERO
    }

    private val logger by getLogger()
    private val hashMap: HashMap<RateIdentifier, Rate> = HashMap<RateIdentifier,Rate>()

    /**
     *  The rate should be set based on the subscription, location, Service-Identifier
     *  and the Rating-Group.
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