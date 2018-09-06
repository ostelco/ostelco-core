package org.ostelco.prime.pseudonymizer

import org.ostelco.prime.model.ActiveMsisdnPseudonyms
import org.ostelco.prime.model.MsisdnPseudonymEntity

interface PseudonymizerService {

    fun getActivePseudonymsForMsisdn(msisdn: String): ActiveMsisdnPseudonyms

    fun getMsisdnPseudonymEntityFor(msisdn: String, timestamp: Long): MsisdnPseudonymEntity
}