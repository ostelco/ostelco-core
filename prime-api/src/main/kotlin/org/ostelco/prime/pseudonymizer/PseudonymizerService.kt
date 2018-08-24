package org.ostelco.prime.pseudonymizer

import org.ostelco.prime.model.ActivePseudonyms

interface PseudonymizerService {
    fun getActivePseudonymsForMsisdn(msisdn: String): ActivePseudonyms
}