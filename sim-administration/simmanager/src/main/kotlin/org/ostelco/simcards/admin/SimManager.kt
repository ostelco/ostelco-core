package org.ostelco.simcards.admin

import arrow.core.Either
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.SimEntry
import org.ostelco.prime.model.SimProfileStatus
import org.ostelco.prime.sim.SimManager
import org.ostelco.simcards.admin.ApiRegistry.simInventoryApi
import org.ostelco.simcards.admin.ApiRegistry.simProfileStatusUpdateCallback
import org.ostelco.simcards.inventory.SmDpPlusState.ALLOCATED
import org.ostelco.simcards.inventory.SmDpPlusState.AVAILABLE
import org.ostelco.simcards.inventory.SmDpPlusState.CONFIRMED
import org.ostelco.simcards.inventory.SmDpPlusState.DOWNLOADED
import org.ostelco.simcards.inventory.SmDpPlusState.ENABLED
import org.ostelco.simcards.inventory.SmDpPlusState.INSTALLED
import org.ostelco.simcards.inventory.SmDpPlusState.RELEASED

class ESimManager : SimManager by SimManagerSingleton

object SimManagerSingleton : SimManager {

    private val logger by getLogger()

    override fun allocateNextEsimProfile(hlr: String, phoneType: String?): Either<String, SimEntry> =
            simInventoryApi.allocateNextEsimProfile(hlrName = hlr, phoneType = phoneType ?: "iphone").bimap(
                    {
                        "Failed to allocate eSIM for HLR - $hlr for phoneType - $phoneType"
                    },
                    { simEntry -> mapToModelSimEntry(simEntry) })

    override fun getSimProfile(hlr: String, iccId: String): Either<String, SimEntry> {
        return simInventoryApi.findSimProfileByIccid(hlrName = hlr, iccid = iccId)
                .map { simEntry -> mapToModelSimEntry(simEntry) }
                .mapLeft {
                    logger.error("Failed to get SIM Profile Status", it.error)
                    it.description
                }
    }

    override fun getSimProfileStatusUpdates(onUpdate: (iccId: String, status: SimProfileStatus) -> Unit) {
        simProfileStatusUpdateCallback = onUpdate
    }

    private fun mapToModelSimEntry(simEntry: org.ostelco.simcards.inventory.SimEntry) : SimEntry {
        val status = when (simEntry.smdpPlusState) {
            AVAILABLE, ALLOCATED, CONFIRMED -> SimProfileStatus.NOT_READY
            RELEASED -> SimProfileStatus.AVAILABLE_FOR_DOWNLOAD
            DOWNLOADED -> SimProfileStatus.DOWNLOADED
            INSTALLED -> SimProfileStatus.INSTALLED
            ENABLED -> SimProfileStatus.ENABLED
        }
        return SimEntry(
                iccId = simEntry.iccid,
                status = status,
                eSimActivationCode = simEntry.code ?: "",
                msisdnList = listOf(simEntry.msisdn))
    }
}