package org.ostelco.simcards.inventory

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.instances.either.monad.flatMap
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.sim.es2plus.ProfileStatus
import org.ostelco.simcards.admin.HlrConfig
import org.ostelco.simcards.admin.ProfileVendorConfig
import org.ostelco.simcards.admin.SimAdministrationConfiguration
import java.io.InputStream


class SimInventoryApi(private val httpClient: CloseableHttpClient,
                      private val config: SimAdministrationConfiguration,
                      private val dao: SimInventoryDAO) {


    fun getSimProfileStatus(hlrName: String, iccid: String): Either<SimManagerError, ProfileStatus> =
            findSimProfileByIccid(hlrName, iccid)
                    .flatMap { simEntry ->
                        dao.getProfileVendorAdapterById(simEntry.profileVendorId)
                                .flatMap { profileVendorAdapter ->
                                    val config: ProfileVendorConfig? = config.profileVendors.firstOrNull {
                                        it.name == profileVendorAdapter.name
                                    }
                                    if (config != null)
                                        profileVendorAdapter.getProfileStatus(httpClient, config, iccid)
                                    else
                                        NotFoundError("")
                                                .left()
                                }
                    }

    fun findSimProfileByIccid(hlrName: String, iccid: String): Either<SimManagerError, SimEntry> =
            dao.getSimProfileByIccid(iccid)
                    .flatMap { simEntry ->
                        dao.getHlrAdapterById(simEntry.hlrId)
                                .flatMap { hlrAdapter ->
                                    if (hlrName != hlrAdapter.name) {
                                        NotFoundError("sim_entries ${hlrName} - ${iccid}")
                                                .left()
                                    } else {
                                        simEntry.right()
                                    }
                                }
                    }

    fun activateHlrProfileByIccid(hlrName: String, iccid: String): Either<SimManagerError, SimEntry> =
            findSimProfileByIccid(hlrName, iccid)
                    .flatMap { simEntry ->
                        dao.getHlrAdapterById(simEntry.hlrId)
                                .flatMap { hlrAdapter ->
                                    val config: HlrConfig? = config.hlrVendors.firstOrNull {
                                        it.name == hlrAdapter.name
                                    }
                                    if (config != null)
                                        when (simEntry.hlrState) {
                                            HlrState.NOT_ACTIVATED -> {
                                                hlrAdapter.activate(httpClient, config, dao, simEntry)
                                            }
                                            HlrState.ACTIVATED -> {
                                                simEntry.right()
                                            }
                                        }
                                    else
                                        NotFoundError("")
                                                .left()
                                }
                    }

    fun deactivateHlrProfileByIccid(hlrName: String, iccid: String): Either<SimManagerError, SimEntry> =
            findSimProfileByIccid(hlrName, iccid)
                    .flatMap { simEntry ->
                        dao.getHlrAdapterById(simEntry.hlrId)
                                .flatMap { hlrAdapter ->
                                    val config: HlrConfig? = config.hlrVendors.firstOrNull {
                                        it.name == hlrAdapter.name
                                    }
                                    if (config != null)
                                        when (simEntry.hlrState) {
                                            HlrState.NOT_ACTIVATED -> {
                                                simEntry.right()
                                                hlrAdapter.activate(httpClient, config, dao, simEntry)
                                            }
                                            HlrState.ACTIVATED -> {
                                                hlrAdapter.deactivate(httpClient, config, dao, simEntry)
                                            }
                                        }
                                    else
                                        NotFoundError("")
                                                .left()
                                }
                    }

    fun findSimProfileByImsi(hlrName: String, imsi: String): Either<SimManagerError, SimEntry> =
            dao.getSimProfileByImsi(imsi)
                    .flatMap { simEntry ->
                        dao.getHlrAdapterById(simEntry.hlrId)
                                .flatMap { hlrAdapter ->
                                    if (hlrName != hlrAdapter.name) {
                                        NotFoundError("sim_entries ${hlrName} - ${imsi}")
                                                .left()
                                    } else {
                                        simEntry.right()
                                    }
                                }

                    }

    fun findSimProfileByMsisdn(hlrName: String, msisdn: String): Either<SimManagerError, SimEntry> =
            dao.getSimProfileByMsisdn(msisdn)
                    .flatMap { simEntry ->
                        dao.getHlrAdapterById(simEntry.hlrId)
                                .flatMap { hlrAdapter ->
                                    if (hlrName != hlrAdapter.name) {
                                        NotFoundError("sim_entries ${hlrName} - ${msisdn}")
                                                .left()
                                    } else {
                                        simEntry.right()
                                    }
                                }
                    }

    fun activateNextEsimProfile(hlrName: String, phoneType: String): Either<SimManagerError, SimEntry> =
            dao.getHlrAdapterByName(hlrName)
                    .flatMap { hlrAdapter ->
                        val profile: String? = config.getProfileForPhoneType(phoneType)

                        if (profile != null) {
                            dao.findNextNonProvisionedSimProfileForHlr(hlrAdapter.id, profile)
                                    .flatMap { simEntry ->
                                        dao.getProfileVendorAdapterById(simEntry.profileVendorId)
                                                .flatMap { profileVendorAdapter ->
                                                    val config: ProfileVendorConfig? = config.profileVendors.firstOrNull {
                                                        it.name == profileVendorAdapter.name
                                                    }
                                                    if (config != null) {
                                                        when (simEntry.smdpPlusState) {
                                                            SmDpPlusState.AVAILABLE -> {
                                                                profileVendorAdapter.activate(httpClient, config, dao, null, simEntry)
                                                            }
                                                            SmDpPlusState.ALLOCATED -> {
                                                                profileVendorAdapter.confirmOrder(httpClient, config, dao, null, simEntry)
                                                            }
                                                            /* ESIM already 'released'. */
                                                            else -> {
                                                                simEntry.right()
                                                            }
                                                        }.flatMap { updatedSimEntry ->
                                                            activateHlrProfileByIccid(hlrName, updatedSimEntry.iccid)
                                                        }
                                                    } else {
                                                        NotFoundError("")
                                                                .left()
                                                    }
                                                }
                                    }
                        } else {
                            NotFoundError("Could not find configuration for phone type ${phoneType}")
                                    .left()
                        }
                    }

    fun allocateNextEsimProfile(hlrName: String, phoneType: String): Either<SimManagerError, SimEntry> =
            dao.getHlrAdapterByName(hlrName)
                    .flatMap { hlrAdapter ->
                        val profile: String? = config.getProfileForPhoneType(phoneType)

                        if (profile != null) {
                            dao.findNextReadyToUseSimProfileForHlr(hlrAdapter.id, profile)
                                    .flatMap { simEntry ->
                                        dao.getProfileVendorAdapterById(simEntry.profileVendorId)
                                                .flatMap { profileVendorAdapter ->
                                                    val config: ProfileVendorConfig? = config.profileVendors.firstOrNull {
                                                        it.name == profileVendorAdapter.name
                                                    }
                                                    if (config != null) {
                                                        dao.setProvisionState(simEntry.id!!, ProvisionState.PROVISIONED)
                                                                .flatMap {
                                                                    it.copy(code = "LPA:${config.es9plusEndpoint}:${it.matchingId}")
                                                                            .right()
                                                                }
                                                    } else {
                                                        NotFoundError("")
                                                                .left()
                                                    }
                                                }
                                    }
                        } else {
                            NotFoundError("Could not find configuration for phone type ${phoneType}")
                                    .left()
                        }
                    }

    fun importBatch(hlrName: String, simVendor: String, csvInputStream: InputStream): Either<SimManagerError, SimImportBatch> =
            dao.getProfileVendorAdapterByName(simVendor)
                    .flatMap { profileVendorAdapter ->
                        dao.getHlrAdapterByName(hlrName)
                                .flatMap { hlrAdapter ->
                                    dao.simVendorIsPermittedForHlr(profileVendorAdapter.id, hlrAdapter.id)
                                            .flatMap {
                                                dao.importSims(importer = "importer", // TODO: This is a very strange name for an importer .-)
                                                        hlrId = hlrAdapter.id,
                                                        profileVendorId = profileVendorAdapter.id,
                                                        csvInputStream = csvInputStream)
                                            }
                                }
                    }
}
