package org.ostelco.prime.pseudonymizer

import org.ostelco.prime.model.ActivePseudonyms
import org.ostelco.prime.model.PseudonymEntity

interface PseudonymizerService {

    fun getActivePseudonymsForMsisdn(msisdn: String): ActivePseudonyms

    fun getMsisdnPseudonym(msisdn: String, timestamp: Long): PseudonymEntity

    fun getSubscriberIdPseudonym(subscriberId: String, timestamp: Long): PseudonymEntity

}