package org.ostelco.simcards.admin

import arrow.core.Either
import arrow.core.fix
import arrow.core.left
import arrow.effects.IO
import arrow.instances.either.monad.flatMap
import arrow.instances.either.monad.monad
import com.google.common.collect.ImmutableMultimap
import io.dropwizard.servlets.tasks.Task
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.getLogger
import org.ostelco.prime.simmanager.NotFoundError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.simcards.hss.HssEntry
import org.ostelco.simcards.hss.SimManagerToHssDispatcherAdapter
import org.ostelco.simcards.inventory.HssState
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SimProfileKeyStatistics
import java.io.PrintWriter


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
    }

    private fun preProvisionSimProfile(hssEntry: HssEntry,
                                       simEntry: SimEntry): Either<SimManagerError, Any> =
            simInventoryDAO.getProfileVendorAdapterById(simEntry.profileVendorId)
                    .flatMap { profileVendorAdapter ->

                        val profileVendorConfig: ProfileVendorConfig? = profileVendors.firstOrNull {
                            it.name == profileVendorAdapter.name
                        }

                        if (profileVendorConfig != null) {
                            profileVendorAdapter.activate(httpClient = httpClient,
                                    config = profileVendorConfig,
                                    dao = simInventoryDAO,
                                    simEntry = simEntry)
                                    .flatMap {
                                        hssAdapterProxy.activate(simEntry)
                                        simInventoryDAO.setHssState(simEntry.id!!, HssState.ACTIVATED)
                                    }
                        } else {
                            NotFoundError("Failed to find configuration for SIM profile vendor ${profileVendorAdapter.name} " +
                                    "and HLR ${hssEntry.name}")
                                    .left()
                        }
                    }

    private fun batchPreprovisionSimProfiles(hssEntry: HssEntry,
                                             simProfileName: String,
                                             profileStats: SimProfileKeyStatistics) {
        val noOfProfilesToActuallyAllocate =
                Math.min(maxNoOfProfileToAllocate.toLong(), profileStats.noOfUnallocatedEntries)

        for (i in 1..noOfProfilesToActuallyAllocate) {

            // XXX This is all well, if allocation doesn't fail, but if it fails, it will try the
            //     same profile forever, so something else should be done e.g. setting the
            //     state of the profile to "provision failed" or something of that nature, so that it
            //     is possible to move along.   This is an error in the logic of this code.
            simInventoryDAO.findNextNonProvisionedSimProfileForHss(hssId = hssEntry.id, profile = simProfileName)
                    .flatMap { simEntry ->
                        preProvisionSimProfile(hssEntry, simEntry)
                                .mapLeft {
                                    logger.error("Preallocation of SIM ICCID {} failed with error: {}}",
                                            simEntry.iccid, it.description)
                                }
                    }
        }
    }

    /**
     * Made public to be testable.   Perform
     * allocation of profiles so that if possible, there will be tasks available for
     * provisioning.
     */
    public fun preAllocateSimProfiles() {
        IO {
            Either.monad<SimManagerError>().binding {
                val hssEntries: Collection<HssEntry> = simInventoryDAO.getHssEntries()
                        .bind()

                for (hssEntry in hssEntries) {
                    val simProfileNames: Collection<String> = simInventoryDAO.getProfileNamesForHssById(hssEntry.id)
                            .bind()
                    for (simProfileName in simProfileNames) {
                        val profileStats = simInventoryDAO.getProfileStats(hssEntry.id, simProfileName)
                                .bind()

                        if (profileStats.noOfEntriesAvailableForImmediateUse < lowWaterMark) {
                            logger.info("Preallocating new SIM batch with HLR {} and with profile {}",
                                    hssEntry.name, simProfileName)

                            batchPreprovisionSimProfiles(hssEntry = hssEntry, simProfileName = simProfileName, profileStats = profileStats)
                        }
                    }
                }
            }.fix()
        }.unsafeRunSync()
    }
}

