package org.ostelco.simcards.inventory

import arrow.core.Either
import arrow.core.fix
import arrow.core.left
import arrow.core.right
import arrow.effects.IO
import arrow.instances.either.monad.flatMap
import arrow.instances.either.monad.monad
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.simmanager.NotFoundError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.sim.es2plus.ProfileStatus
import org.ostelco.simcards.adapter.HlrAdapter
import org.ostelco.simcards.adapter.ProfileVendorAdapter
import org.ostelco.simcards.admin.HlrConfig
import org.ostelco.simcards.admin.ProfileVendorConfig
import org.ostelco.simcards.admin.SimAdministrationConfiguration
import java.io.InputStream


class SimInventoryApi(private val httpClient: CloseableHttpClient,
                      private val config: SimAdministrationConfiguration,
                      private val dao: SimInventoryDAO) {

    fun findSimProfileByIccid(hlrName: String, iccid: String): Either<SimManagerError, SimEntry> =
            dao.getSimProfileByIccid(iccid)
                    .flatMap { simEntry ->
                        checkForValidHlr(hlrName, simEntry)
                    }

    fun findSimProfileByImsi(hlrName: String, imsi: String): Either<SimManagerError, SimEntry> =
            dao.getSimProfileByImsi(imsi)
                    .flatMap { simEntry ->
                        checkForValidHlr(hlrName, simEntry)
                    }

    fun findSimProfileByMsisdn(hlrName: String, msisdn: String): Either<SimManagerError, SimEntry> =
            dao.getSimProfileByMsisdn(msisdn)
                    .flatMap { simEntry ->
                        checkForValidHlr(hlrName, simEntry)
                    }

    fun getSimProfileStatus(hlrName: String, iccid: String): Either<SimManagerError, ProfileStatus> =
            findSimProfileByIccid(hlrName, iccid)
                    .flatMap { simEntry ->
                        getProfileVendorAdapterAndConfig(simEntry)
                                .flatMap {
                                    it.first.getProfileStatus(httpClient, it.second, iccid)
                                }
                    }

    fun activateHlrProfileByIccid(hlrName: String, iccid: String): Either<SimManagerError, SimEntry> =
            findSimProfileByIccid(hlrName, iccid)
                    .flatMap { simEntry ->
                        getHlrAdapterAndConfig(simEntry)
                                .flatMap {
                                    when (simEntry.hlrState) {
                                        HlrState.NOT_ACTIVATED -> {
                                            it.first.activate(httpClient, it.second, dao, simEntry)
                                        }
                                        HlrState.ACTIVATED -> {
                                            simEntry.right()
                                        }
                                    }
                                }
                    }

    fun deactivateHlrProfileByIccid(hlrName: String, iccid: String): Either<SimManagerError, SimEntry> =
            findSimProfileByIccid(hlrName, iccid)
                    .flatMap { simEntry ->
                        getHlrAdapterAndConfig(simEntry)
                                .flatMap {
                                    when (simEntry.hlrState) {
                                        HlrState.NOT_ACTIVATED -> {
                                            simEntry.right()
                                        }
                                        HlrState.ACTIVATED -> {
                                            it.first.deactivate(httpClient, it.second, dao, simEntry)
                                        }
                                    }
                                }
                    }

    fun activateNextEsimProfile(hlrName: String, phoneType: String): Either<SimManagerError, SimEntry> =
            IO {
                Either.monad<SimManagerError>().binding {
                    val hlrAdapter = dao.getHlrAdapterByName(hlrName)
                            .bind()
                    val profile = getProfileForPhoneType(phoneType)
                            .bind()
                    val simEntry = dao.findNextNonProvisionedSimProfileForHlr(hlrAdapter.id, profile)
                            .bind()
                    val profileVendorAndConfig = getProfileVendorAdapterAndConfig(simEntry)
                            .bind()

                    val vendorAdapter = profileVendorAndConfig.first
                    val config = profileVendorAndConfig.second

                    /* As 'confirm-order' message is issued with 'releaseFlag' set to true, the
                       CONFIRMED state should not occur. */
                    val updatedSimEntry: SimEntry = when (simEntry.smdpPlusState) {
                        SmDpPlusState.AVAILABLE -> {
                            vendorAdapter.activate(httpClient, config, dao, null, simEntry)
                                    .bind()
                        }
                        SmDpPlusState.ALLOCATED -> {
                            vendorAdapter.confirmOrder(httpClient, config, dao, null, simEntry)
                                    .bind()
                        }
                        /* ESIM already 'released'. */
                        else -> {
                            simEntry
                        }
                    }

                    /* Enable SIM profile with HLR. */
                    activateHlrProfileByIccid(hlrName, updatedSimEntry.iccid)
                            .bind()
                }.fix()
            }.unsafeRunSync()

    fun allocateNextEsimProfile(hlrName: String, phoneType: String): Either<SimManagerError, SimEntry> =
            IO {
                Either.monad<SimManagerError>().binding {
                    val hlrAdapter = dao.getHlrAdapterByName(hlrName)
                            .bind()
                    val profile = getProfileForPhoneType(phoneType)
                            .bind()
                    val simEntry = dao.findNextReadyToUseSimProfileForHlr(hlrAdapter.id, profile)
                            .bind()
                    val profileVendorAndConfig = getProfileVendorAdapterAndConfig(simEntry)
                            .bind()

                    val config = profileVendorAndConfig.second

                    dao.setProvisionState(simEntry.id!!, ProvisionState.PROVISIONED)
                            .flatMap {
                                /* Add 'code' field content. */
                                it.copy(code = "LPA:${config.es9plusEndpoint}:${it.matchingId}")
                                        .right()
                            }.bind()
                }.fix()
            }.unsafeRunSync()

    fun importBatch(hlrName: String, simVendor: String, csvInputStream: InputStream): Either<SimManagerError, SimImportBatch> =
            IO {
                Either.monad<SimManagerError>().binding {
                    val profileVendorAdapter = dao.getProfileVendorAdapterByName(simVendor)
                            .bind()
                    val hlrAdapter = dao.getHlrAdapterByName(hlrName)
                            .bind()

                    /* Exits if not true. */
                    dao.simVendorIsPermittedForHlr(profileVendorAdapter.id, hlrAdapter.id)
                            .bind()
                    dao.importSims(importer = "importer", // TODO: This is a very strange name for an importer .-)
                            hlrId = hlrAdapter.id,
                            profileVendorId = profileVendorAdapter.id,
                            csvInputStream = csvInputStream).bind()
                }.fix()
            }.unsafeRunSync()

    /* Helper functions. */

    private fun checkForValidHlr(hlrName: String, simEntry: SimEntry): Either<SimManagerError, SimEntry> =
            dao.getHlrAdapterById(simEntry.hlrId)
                    .flatMap { hlrAdapter ->
                        if (hlrName != hlrAdapter.name)
                            NotFoundError("HLR name ${hlrName} does not match SIM profile HLR ${hlrAdapter.name}")
                                    .left()
                        else
                            simEntry.right()
                    }

    private fun getHlrAdapterAndConfig(simEntry: SimEntry): Either<SimManagerError, Pair<HlrAdapter, HlrConfig>> =
            dao.getHlrAdapterById(simEntry.hlrId)
                    .flatMap { hlrAdapter ->
                        val config: HlrConfig? = config.hlrVendors.firstOrNull {
                            it.name == hlrAdapter.name
                        }
                        if (config != null)
                            Pair(hlrAdapter, config).right()
                        else
                            NotFoundError("Could not find configuration for HLR ${hlrAdapter.name}")
                                    .left()
                    }

    private fun getProfileVendorAdapterAndConfig(simEntry: SimEntry): Either<SimManagerError, Pair<ProfileVendorAdapter, ProfileVendorConfig>> =
            dao.getProfileVendorAdapterById(simEntry.profileVendorId)
                    .flatMap { profileVendorAdapter ->
                        val config: ProfileVendorConfig? = config.profileVendors.firstOrNull {
                            it.name == profileVendorAdapter.name
                        }
                        if (config != null)
                            Pair(profileVendorAdapter, config).right()
                        else
                            NotFoundError("Could not find configuration for SIM profile vendor ${profileVendorAdapter.name}")
                                    .left()
                    }

    private fun getProfileForPhoneType(phoneType: String): Either<SimManagerError, String> {
        val profile: String? = config.getProfileForPhoneType(phoneType)
        return if (profile != null)
            profile.right()
        else
            NotFoundError("Could not find configuration for phone type ${phoneType}")
                    .left()
    }
}
