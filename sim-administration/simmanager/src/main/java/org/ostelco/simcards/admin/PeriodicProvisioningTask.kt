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
import org.ostelco.prime.simmanager.NotFoundError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.simcards.hss.HssEntry
import org.ostelco.simcards.hss.HssProxy
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SimProfileKeyStatistics
import org.slf4j.LoggerFactory
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
        val lowWaterMark: Int = 10,
        val maxNoOfProfileToAllocate: Int = 30,
        val simInventoryDAO: SimInventoryDAO,
        val httpClient: CloseableHttpClient,
        val hssAdapterProxy: HssProxy,
        val profileVendors: List<ProfileVendorConfig>) : Task("preallocate_sim_profiles") {

    private val log = LoggerFactory.getLogger(javaClass)

    @Throws(Exception::class)
    override fun execute(parameters: ImmutableMultimap<String, String>, output: PrintWriter) {
        preAllocateSimProfiles()
    }

    private fun preProvisionSimProfile(hssEntry: HssEntry,
                                       simEntry: SimEntry): Either<SimManagerError, SimEntry> =
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
                                    }
                        } else {
                            if (profileVendorConfig == null) {
                                NotFoundError("Failed to find configuration for SIM profile vendor ${profileVendorAdapter.name}")
                                        .left()
                            } else {
                                NotFoundError("Failed to find configuration for SIM profile vendor ${profileVendorAdapter.name} " +
                                        "and HLR ${hssEntry.name}")
                                        .left()
                            }
                        }
                    }

    private fun batchPreprovisionSimProfiles(hlrEntry: HssEntry,
                                             profile: String,
                                             profileStats: SimProfileKeyStatistics) {
        val noOfProfilesToActuallyAllocate =
                Math.min(maxNoOfProfileToAllocate.toLong(), profileStats.noOfUnallocatedEntries)

        for (i in 1..noOfProfilesToActuallyAllocate) {
            simInventoryDAO.findNextNonProvisionedSimProfileForHss(hssId = hlrEntry.id, profile = profile)
                    .flatMap {
                        preProvisionSimProfile(hlrEntry, it)
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

                for (entry in hssEntries) {
                    val profiles: Collection<String> = simInventoryDAO.getProfileNamesForHssById(entry.id)
                            .bind()
                    for (profile in profiles) {
                        val profileStats = simInventoryDAO.getProfileStats(entry.id, profile)
                                .bind()

                        if (profileStats.noOfEntriesAvailableForImmediateUse < lowWaterMark) {
                            batchPreprovisionSimProfiles(hlrEntry = entry, profile = profile, profileStats = profileStats)
                        }
                    }
                }
            }.fix()
        }.unsafeRunSync()
    }
}

