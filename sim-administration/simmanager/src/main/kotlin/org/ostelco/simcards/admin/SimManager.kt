package org.ostelco.simcards.admin

import arrow.core.Either
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.SimEntry
import org.ostelco.prime.model.SimProfileStatus
import org.ostelco.prime.sim.SimManager
import org.ostelco.simcards.admin.ApiRegistry.simInventoryApi
import org.ostelco.simcards.admin.ApiRegistry.simProfileStatusUpdateListeners
import org.ostelco.simcards.inventory.SmDpPlusState

class ESimManager : SimManager by SimManagerSingleton

object SimManagerSingleton : SimManager {

    private val logger by getLogger()

    override fun allocateNextEsimProfile(hssName: String, phoneType: String?): Either<String, SimEntry> =
            simInventoryApi.allocateNextEsimProfile(hssName = hssName, phoneType = "$hssName.${phoneType ?: "generic"}").bimap(
                    {
                        "Failed to allocate eSIM for HLR - $hssName for phoneType - $phoneType"
                    },
                    { simEntry -> mapToModelSimEntry(simEntry) })

    override fun getSimProfile(hlr: String, iccId: String): Either<String, SimEntry> {
        return simInventoryApi.findSimProfileByIccid(hlrName = hlr, iccid = iccId)
                .map { simEntry -> mapToModelSimEntry(simEntry) }
                .mapLeft {
                    logger.error("Failed to get SIM Profile, hlr = {}, ICCID = {},  description: {}", iccId, hlr, it.description)
                    it.description
                }
    }

    override fun addSimProfileStatusUpdateListener(listener: (iccId: String, status: SimProfileStatus) -> Unit) {
        simProfileStatusUpdateListeners.add(listener)
    }

    private fun mapToModelSimEntry(simEntry: org.ostelco.simcards.inventory.SimEntry) : SimEntry {

        val status = asSimProfileStatus(simEntry.smdpPlusState)
        return SimEntry(
                iccId = simEntry.iccid,
                status = status,
                eSimActivationCode = simEntry.code ?: "",
                msisdnList = listOf(simEntry.msisdn))
    }

    fun asSimProfileStatus(smdpPlusState: SmDpPlusState) : SimProfileStatus {
        return when (smdpPlusState) {
            SmDpPlusState.AVAILABLE -> SimProfileStatus.NOT_READY
            SmDpPlusState.ALLOCATED -> SimProfileStatus.NOT_READY
            SmDpPlusState.CONFIRMED -> SimProfileStatus.NOT_READY
            SmDpPlusState.RELEASED -> SimProfileStatus.AVAILABLE_FOR_DOWNLOAD
            SmDpPlusState.DOWNLOADED -> SimProfileStatus.DOWNLOADED
            SmDpPlusState.INSTALLED -> SimProfileStatus.INSTALLED
            SmDpPlusState.ENABLED -> SimProfileStatus.ENABLED
            SmDpPlusState.DELETED -> SimProfileStatus.DELETED
        }
    }
}