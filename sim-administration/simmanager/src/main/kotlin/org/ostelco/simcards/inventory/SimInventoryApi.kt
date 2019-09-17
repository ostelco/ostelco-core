package org.ostelco.simcards.inventory

import arrow.core.Either
import arrow.core.fix
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import arrow.effects.IO
import arrow.instances.either.monad.monad
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.getLogger
import org.ostelco.prime.simmanager.DatabaseError
import org.ostelco.prime.simmanager.NotFoundError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.sim.es2plus.ProfileStatus
import org.ostelco.simcards.admin.ProfileVendorConfig
import org.ostelco.simcards.admin.SimAdministrationConfiguration
import org.ostelco.simcards.profilevendors.ProfileVendorAdapter
import java.io.InputStream


class SimInventoryApi(private val httpClient: CloseableHttpClient,
                      private val simAdminConfig: SimAdministrationConfiguration,
                      private val dao: SimInventoryDAO) {

    private val logger by getLogger()

    fun findSimProfileByIccid(hlrName: String, iccid: String): Either<SimManagerError, SimEntry> =
            IO {
                Either.monad<SimManagerError>().binding {

                    val simEntry = dao.getSimProfileByIccid(iccid).bind()
                    checkForValidHlr(hlrName, simEntry)

                    val config = getProfileVendorConfig(simEntry).bind()

                    // Return the entry found in the database, extended with a
                    // code represernting the string that will be used by the LPA in the
                    // UA to talk to the sim vendor's SM-DP+ over the ES9+ protocol.
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


    // TODO: Rewrite this function to not use the "getProfileVendorAdapterAndConfig" but instead simply use
    //       getProfileVendorAdapter, also get rid of the wretched nested flatmaps. They are an abomination!
    fun getSimProfileStatus(hlrName: String, iccid: String): Either<SimManagerError, ProfileStatus> =
            // TODO: This looks odd, can it be elvised into something more compact?
            findSimProfileByIccid(hlrName, iccid)
                    .flatMap { simEntry ->
                        getProfileVendorAdapter(simEntry)
                                .flatMap {
                                    it.getProfileStatus(iccid = iccid)
                                }
                    }

    fun allocateNextEsimProfile(hlrName: String, phoneType: String): Either<SimManagerError, SimEntry> =
            IO {
                Either.monad<SimManagerError>().binding {
                    logger.info("Allocating new SIM for hlr ${hlrName} and phone-type ${phoneType}")

                    val hlrAdapter = dao.getHssEntryByName(hlrName)
                            .bind()
                    val profile = getProfileType(hlrName, phoneType)
                            .bind()
                    val simEntry = dao.findNextReadyToUseSimProfileForHss(hlrAdapter.id, profile)
                            .bind()
                    val config = getProfileVendorConfig(simEntry)
                            .bind()

                    if (simEntry.id == null) {
                        DatabaseError("simEntry has no id (simEntry=$simEntry)").left().bind()
                    }

                    val updatedSimEntry = dao.setProvisionState(simEntry.id, ProvisionState.PROVISIONED)
                            .bind()

                    // TODO: Add 'code' field content.
                    //   Original format: LPA:<hostname>:<matching-id>
                    //   New format: LPA:1$<endpoint>$<matching-id> */
                    updatedSimEntry.copy(code = "LPA:1\$${config.es9plusEndpoint}\$${updatedSimEntry.matchingId}")
                }.fix()
            }.unsafeRunSync()



    fun importBatch(hlrName: String,
                    simVendor: String,
                    csvInputStream: InputStream,
                    initialHssState: HssState): Either<SimManagerError, SimImportBatch> =
            IO {
                Either.monad<SimManagerError>().binding {
                    val profileVendorAdapter = dao.getProfileVendorAdapterDatumByName(simVendor)
                            .bind()
                    val hlrAdapter = dao.getHssEntryByName(hlrName)
                            .bind()

                    /* Exits if not true. */
                    dao.simVendorIsPermittedForHlr(profileVendorAdapter.id, hlrAdapter.id)
                            .bind()
                    dao.importSims(importer = "importer", // TODO: This is a very strange metricName for an importer .-)
                            hlrId = hlrAdapter.id,
                            profileVendorId = profileVendorAdapter.id,
                            csvInputStream = csvInputStream,
                            initialHssState = initialHssState).bind()
                }.fix()
            }.unsafeRunSync()

    /* Helper functions. */

    private fun checkForValidHlr(hlrName: String, simEntry: SimEntry): Either<SimManagerError, SimEntry> =
            dao.getHssEntryById(simEntry.hssId)
                    .flatMap { hlrAdapter ->
                        if (hlrName != hlrAdapter.name)
                            NotFoundError("HLR metricName $hlrName does not match SIM profile HLR ${hlrAdapter.name}")
                                    .left()
                        else
                            simEntry.right()
                    }

    private fun getProfileVendorConfig(simEntry: SimEntry): Either<SimManagerError, ProfileVendorConfig> =
            dao.getProfileVendorAdapterDatumById(simEntry.profileVendorId)
                    .flatMap { profileVendorAdapterDatum ->
                        val config: ProfileVendorConfig? = simAdminConfig.profileVendors.firstOrNull {
                            it.name == profileVendorAdapterDatum.name
                        }
                        if (config != null)
                            config.right()
                        else
                            NotFoundError("Could not find configuration for SIM profile vendor ${profileVendorAdapterDatum.name}")
                                    .left()
                    }


    private fun getProfileVendorAdapter(simEntry: SimEntry): Either<SimManagerError, ProfileVendorAdapter> =
            dao.getProfileVendorAdapterDatumById(simEntry.profileVendorId)
                    .flatMap { profileVendorAdapterDatum ->
                        getProfileVendorConfig(simEntry).flatMap { profileConfig ->
                            ProfileVendorAdapter(profileVendorAdapterDatum, profileConfig, httpClient, dao).right()
                        }
                    }

    private fun getProfileType(hlrName: String, phoneType: String): Either<SimManagerError, String> = simAdminConfig
            .getProfileForPhoneType(phoneType)
            ?.right()
            ?: NotFoundError("Could not find configuration for phone type='$phoneType', hlrName='$hlrName'").left()
}
