package org.ostelco.prime.pseudonymizer

import org.ostelco.prime.model.ActivePseudonyms
import org.ostelco.prime.model.PseudonymEntity

interface PseudonymizerService {

    fun getActivePseudonymsForMsisdn(msisdn: String): ActivePseudonyms

    fun getPseudonymEntityFor(msisdn: String, timestamp: Long): PseudonymEntity
}