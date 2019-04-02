package org.ostelco.simcards.admin

import arrow.core.Either
import org.ostelco.prime.model.SimEntry
import org.ostelco.prime.sim.SimManager
import org.ostelco.simcards.admin.ApiRegistry.simInventoryApi

object ESimManager : SimManager {

    override fun allocateNextEsimProfile(hlr: String, phoneType: String): Either<String, SimEntry> =
            simInventoryApi.allocateNextEsimProfile(hlrName = hlr, phoneType = phoneType).bimap(
                    {
                        "Failed to allocate eSIM for HLR - $hlr for phoneType - $phoneType"
                    },
                    { simEntry ->
                        SimEntry(iccId = simEntry.iccid,
                                eSimActivationCode = simEntry.code!!,
                                msisdnList = listOf(simEntry.msisdn))
                    })
}