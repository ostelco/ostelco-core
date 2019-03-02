package org.ostelco.simcards.admin

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.SimEntry
import org.ostelco.prime.sim.SimManager
import org.ostelco.simcards.admin.ResourceRegistry.simInventoryResource

object ESimManager : SimManager {

    private val logger by getLogger()

    override fun allocateNextEsimProfile(hlr: String, phoneType: String): Either<String, SimEntry> {

        return try {
            simInventoryResource.allocateNextEsimProfile(hlr = hlr, phoneType = phoneType)
                    ?.let {
                        val msisdn = it.msisdn
                        it.code
                                ?.let { eSimActivationCode ->
                                    SimEntry(msisdn = msisdn, eSimActivationCode = eSimActivationCode).right()
                                }
                                ?: "Missing eSim ActivationCode in allocated eSIM for HLR - $hlr for phoneType - $phoneType".left()
                    }
                    ?: "Failed to allocate eSIM for HLR - $hlr for phoneType - $phoneType".left()
        } catch (e: Exception) {
            logger.error("Exception occurred when allocating eSIM for HLR - $hlr for phoneType - $phoneType", e)
            "Exception occurred when allocating eSIM for HLR  - $hlr for phoneType - $phoneType".left()
        }
    }
}