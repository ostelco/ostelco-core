package org.ostelco.simcards.admin

import com.google.common.collect.ImmutableMultimap
import io.dropwizard.servlets.tasks.Task
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.simcards.adapter.HssAdapter
import org.ostelco.simcards.adapter.HssEntry
import org.ostelco.simcards.adapter.Wg2HssAdapter
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SimProfileKeyStatistics
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import javax.ws.rs.WebApplicationException


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
        val hssAdapters: HssAdapterCache,
        val profileVendors: List<ProfileVendorConfig>) : Task("preallocate_sim_profiles") {

    private val log = LoggerFactory.getLogger(javaClass)

    @Throws(Exception::class)
    override fun execute(parameters: ImmutableMultimap<String, String>, output: PrintWriter) {
        preallocateProfiles()
    }


    fun doPreprovisioning(hssEntry: HssEntry,
                          profile: String,
                          hssAdapters: HssAdapterCache,
                          profileStats: SimProfileKeyStatistics) {
        val noOfProfilesToActuallyAllocate =
                Math.min(maxNoOfProfileToAllocate.toLong(), profileStats.noOfUnallocatedEntries)

        for (i in 1..noOfProfilesToActuallyAllocate) {
            val simEntry =
                    simInventoryDAO.findNextNonProvisionedSimProfileForHlr(
                            hssId = hssEntry.id,
                            profile = profile)

            if (simEntry == null) {
                throw WebApplicationException("Could not find SIM profile for hlr '${hssEntry.name}' matching profile '${profile}'")
            }

            val simVendorAdapter =
                    simInventoryDAO.getProfileVendorAdapterById(simEntry.profileVendorId)

            if (simVendorAdapter == null) {
                throw WebApplicationException("Could not find SIM vendor adapter matching id '${simEntry.profileVendorId}'")
            }

            val profileVendorConfig = profileVendors.find { it.name == simVendorAdapter.name }!!

            simVendorAdapter.downloadOrder(httpClient = httpClient, dao = simInventoryDAO, simEntry = simEntry, config = profileVendorConfig)
            simVendorAdapter.confirmOrder(httpClient = httpClient, dao = simInventoryDAO, simEntry = simEntry, config = profileVendorConfig)

            val hssAdapterById = hssAdapters.getHssAdapterById(simEntry.hssId)
            hssAdapterById!!.activate(simEntry)
        }
    }


    /**
     * Made public to be testable.   Perform
     * allocation of profiles so that if possible, there will be tasks available for
     * provisioning.
     */
    public fun preallocateProfiles() {

        hssAdapters.initialize()

        for (hssEntry in hssAdapters.getHssEntries()) {
            val profiles: Collection<String> = simInventoryDAO.getProfileNamesForHlr(hssEntry.id)
            for (profile in profiles) {
                val profileStats =
                        simInventoryDAO.getProfileStats(hssEntry.id, profile)
                if (profileStats.noOfEntriesAvailableForImmediateUse < lowWaterMark) {
                    doPreprovisioning(hssAdapters = hssAdapters, hssEntry = hssEntry, profile = profile, profileStats = profileStats)
                }
            }
        }
    }
}

