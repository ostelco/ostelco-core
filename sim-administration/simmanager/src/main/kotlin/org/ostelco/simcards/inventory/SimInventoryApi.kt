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
import org.ostelco.simcards.admin.ProfileVendorConfig
import org.ostelco.simcards.admin.SimAdministrationConfiguration
import org.ostelco.simcards.profilevendors.ProfileVendorAdapter
import java.io.InputStream


class SimInventoryApi(private val httpClient: CloseableHttpClient,
                      private val config: SimAdministrationConfiguration,
                      private val dao: SimInventoryDAO) {

    fun findSimProfileByIccid(hlrName: String, iccid: String): Either<SimManagerError, SimEntry> =
            IO {
                Either.monad<SimManagerError>().binding {

                    val simEntry = dao.getSimProfileByIccid(iccid).bind()

                    checkForValidHlr(hlrName, simEntry)

                    val profileVendorAndConfig = getProfileVendorAdapterAndConfig(simEntry).bind()

                    val config = profileVendorAndConfig.second

                    simEntry.copy(code = "LPA:1\$${config.es9plusEndpoint}\$${simEntry.matchingId}")

                }.fix()
            }.unsafeRunSync()

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


    fun allocateNextEsimProfile(hlrName: String, phoneType: String): Either<SimManagerError, SimEntry> =
            IO {
                Either.monad<SimManagerError>().binding {
                    val hlrAdapter = dao.getHssEntryByName(hlrName)
                            .bind()
                    val profile = getProfileForPhoneType(phoneType)
                            .bind()
                    val simEntry = dao.findNextReadyToUseSimProfileForHss(hlrAdapter.id, profile)
                            .bind()
                    val profileVendorAndConfig = getProfileVendorAdapterAndConfig(simEntry)
                            .bind()

                    val config = profileVendorAndConfig.second

                    val updatedSimEntry = dao.setProvisionState(simEntry.id!!, ProvisionState.PROVISIONED)
                            .bind()

                    /* Add 'code' field content.
                       Original format: LPA:<hostname>:<matching-id>
                       New format: LPA:1$<endpoint>$<matching-id> */
                    updatedSimEntry.copy(code = "LPA:1\$${config.es9plusEndpoint}\$${updatedSimEntry.matchingId}")
                }.fix()
            }.unsafeRunSync()

    fun importBatch(hlrName: String, simVendor: String, csvInputStream: InputStream): Either<SimManagerError, SimImportBatch> =
            IO {
                Either.monad<SimManagerError>().binding {
                    val profileVendorAdapter = dao.getProfileVendorAdapterByName(simVendor)
                            .bind()
                    val hlrAdapter = dao.getHssEntryByName(hlrName)
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
            dao.getHssEntryById(simEntry.hssId)
                    .flatMap { hlrAdapter ->
                        if (hlrName != hlrAdapter.name)
                            NotFoundError("HLR name $hlrName does not match SIM profile HLR ${hlrAdapter.name}")
                                    .left()
                        else
                            simEntry.right()
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
            NotFoundError("Could not find configuration for phone type $phoneType")
                    .left()
    }
}
