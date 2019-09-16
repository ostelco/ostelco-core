package org.ostelco.simcards.admin

import arrow.core.Either
import arrow.core.fix
import arrow.core.left
import arrow.core.right
import arrow.effects.IO
import arrow.instances.either.monad.flatMap
import arrow.instances.either.monad.monad
import com.google.common.collect.ImmutableMultimap
import io.dropwizard.servlets.tasks.Task
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.simmanager.AdapterError
import org.ostelco.prime.simmanager.DatabaseError
import org.ostelco.prime.simmanager.NotFoundError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.prime.simmanager.SystemError
import org.ostelco.simcards.hss.HssEntry
import org.ostelco.simcards.hss.SimManagerToHssDispatcherAdapter
import org.ostelco.simcards.inventory.HssState
import org.ostelco.simcards.inventory.ProvisionState
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SimProfileKeyStatistics
import org.ostelco.simcards.profilevendors.ProfileVendorAdapter
import java.io.PrintWriter
import kotlin.math.min


/**
 * A dropwizard "task" that is intended to be invoked as an administrative step
 * by an external agent that is part of the serving system, not a customer of it.
 *
 * The task implements pre-allocation of profiles in both HLR and SM-DP+ so that
 * there will be a number of profiles available for quick allocation to customers
 * without having to synchronously wait for a profile to be provisioned by these
 * two.
 */

class PreallocateProfilesTask(
        private val lowWaterMark: Int = 10,
        val maxNoOfProfileToAllocate: Int = 30,
        val simInventoryDAO: SimInventoryDAO,
        val httpClient: CloseableHttpClient,
        val hssAdapterProxy: SimManagerToHssDispatcherAdapter,
        val profileVendors: List<ProfileVendorConfig>) : Task("preallocate_sim_profiles") {


    private val logger by getLogger()

    @Throws(Exception::class)
    override fun execute(parameters: ImmutableMultimap<String, String>, output: PrintWriter) {
        preAllocateSimProfiles()
                .mapLeft { simManagerError ->
                    logger.error(simManagerError.description)
                    output.println(asJson(simManagerError))
                }
    }

    // TODO: This method must be refactored. It is _way_ too complex.
    private fun preProvisionSimProfile(hssEntry: HssEntry,
                                       simEntry: SimEntry): Either<SimManagerError, SimEntry> =
            simInventoryDAO.getProfileVendorAdapterDatumById(simEntry.profileVendorId)
                    .flatMap { profileVendorAdapterDatum ->

                        val profileVendorConfig: ProfileVendorConfig? = profileVendors.firstOrNull {
                            it.name == profileVendorAdapterDatum.name
                        }

                        if (profileVendorConfig == null) {
                            AdapterError("profileVendorCondig null for hss $hssEntry, that's very bad.").left()
                        } else {
                            val profileVendorAdapter = ProfileVendorAdapter(profileVendorAdapterDatum, profileVendorConfig, httpClient)

                            when {
                                simEntry.id == null -> SystemError("simEntry.id == null for simEntry = $simEntry").left()
                                profileVendorConfig == null -> NotFoundError("Failed to find configuration for SIM profile vendor ${profileVendorAdapterDatum.name} " +
                                        "and HLR ${hssEntry.name}")
                                        .left()
                                simEntry.hssState == HssState.NOT_ACTIVATED -> {
                                    logger.debug("Preallocating (HSS not activated) for HSS with ID/metricName ${hssEntry.id}/${hssEntry.name} simEntry with ICCID=${simEntry.iccid}")


                                    profileVendorAdapter.activate(
                                            httpClient = httpClient,
                                            dao = simInventoryDAO,
                                            simEntry = simEntry)
                                            .flatMap {
                                                hssAdapterProxy.activate(simEntry)
                                            }
                                            .flatMap {
                                                simInventoryDAO.setHssState(simEntry.id, HssState.ACTIVATED)
                                            }

                                }
                                else -> {
                                    logger.debug("Preallocating (HSS preactivated) for HSS with ID/metricName ${hssEntry.id}/${hssEntry.name} simEntry with ICCID=${simEntry.iccid}")
                                    profileVendorAdapter.activate(
                                            httpClient = httpClient,
                                            dao = simInventoryDAO,
                                            simEntry = simEntry)
                                }
                            }
                        }
                    }


    private fun batchPreprovisionSimProfiles(hssEntry: HssEntry,
                                             simProfileName: String,
                                             profileStats: SimProfileKeyStatistics): Either<SimManagerError, Any> {

        logger.debug("batchPreprovisionSimProfiles hssEntry='$hssEntry', simProfileName='$simProfileName', profileStats='$profileStats.'")

        val noOfProfilesToActuallyAllocate =
                min(maxNoOfProfileToAllocate.toLong(), profileStats.noOfUnallocatedEntries)

        logger.debug("preprovisioning for profileName='$simProfileName', HSS with ID/metricName ${hssEntry.id}/${hssEntry.name}. noOfProfilesToActuallyAllocate= $noOfProfilesToActuallyAllocate")


        if (noOfProfilesToActuallyAllocate == 0L) {
            logger.error("Could not find any profiles to allocate for hssname = '{}', profilename = '{}', profileStats = '{}'",
                    hssEntry.name,
                    simProfileName,
                    profileStats)
        } else
            for (i in 1..noOfProfilesToActuallyAllocate) {

                logger.debug("preprovisioning for profileName='$simProfileName', HSS with ID/metricName ${hssEntry.id}/${hssEntry.name}. Iteration index = $i")
                simInventoryDAO.findNextNonProvisionedSimProfileForHss(hssId = hssEntry.id, profile = simProfileName)
                        .flatMap { simEntry ->
                            logger.debug("preprovisioning for profileName='$simProfileName', HSS with ID/metricName ${hssEntry.id}/${hssEntry.name} simEntry with ICCID=${simEntry.iccid}, id = ${simEntry.id}")
                            if (simEntry.id == null) {
                                DatabaseError("This should never happen, since everything that is read from a database should have an ID")
                                        .left()
                            } else {
                                preProvisionSimProfile(hssEntry, simEntry)
                                        .mapLeft {
                                            logger.error("Preallocation of SIM ICCID {} failed with error: {}}",
                                                    simEntry.iccid, it.description)
                                            simInventoryDAO.setProvisionState(simEntry.id, ProvisionState.ALLOCATION_FAILED)
                                        }

                            }
                        }
            }
        return "ok".right()
    }

    /**
     * Made public to be testable.   Perform
     * allocation of profiles so that if possible, there will be tasks available for
     * provisioning.
     */
    fun preAllocateSimProfiles(): Either<SimManagerError, Unit> =
            IO {
                logger.debug("Start of prealloacation")

                Either.monad<SimManagerError>().binding {
                    val hssEntries: Collection<HssEntry> = simInventoryDAO.getHssEntries()
                            .bind()
                    hssEntries.forEach { hssEntry ->
                        logger.debug("Start of prealloacation for HSS with ID/metricName ${hssEntry.id}/${hssEntry.name}")
                        val simProfileNames: Collection<String> = simInventoryDAO.getProfileNamesForHssById(hssEntry.id)
                                .bind()
                        for (simProfileName in simProfileNames) {
                            logger.debug("Start of prealloacation for HSS with ID/metricName ${hssEntry.id}/${hssEntry.name}, sim profile named '$simProfileName'")

                            val profileStats = simInventoryDAO.getProfileStats(hssEntry.id, simProfileName)
                                    .bind()

                            if (profileStats.noOfUnallocatedEntries == 0L) {
                                logger.error("No  more unallocated  profiles of type $simProfileName for HSS with ID/metricName ${hssEntry.id}/${hssEntry.name}")
                            } else {
                                logger.debug("Profiles ready for use: ${hssEntry.id}/${hssEntry.name}/$simProfileName = ${profileStats.noOfEntriesAvailableForImmediateUse}")
                                if (profileStats.noOfEntriesAvailableForImmediateUse < lowWaterMark) {
                                    logger.info("Preallocating new SIM batch with HLR {} and with profile {}",
                                            hssEntry.name, simProfileName)

                                    batchPreprovisionSimProfiles(hssEntry = hssEntry, simProfileName = simProfileName, profileStats = profileStats)
                                }
                            }
                        }
                    }
                }.fix()
            }.unsafeRunSync()
}

