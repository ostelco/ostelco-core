package org.ostelco.simcards.admin

import com.google.common.collect.ImmutableMultimap
import io.dropwizard.servlets.tasks.Task
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.simcards.adapter.HlrAdapter
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import javax.ws.rs.WebApplicationException


/**
 * A dropwizard "task" that is intended to be invoked as an administrative step
 * by an external agent that is part of the serving system, not a customer of it.
 *
 * The task implements preallocation of profiles in both HLR and SM-DP+ so that
 * there will be a number of profiles available for quick allocation to customers
 * without having to synchronously wait for a profile to be provisioned by these
 * two.x
 */

class PreallocateProfilesTask(
        val lowWaterMark: Int = 10,
        val maxNoOfProfileToAllocate: Int = 30,
        val simInventoryDAO: SimInventoryDAO,
        val httpClient: CloseableHttpClient,
        val hlrConfigs: List<HlrConfig>,
        val profileVendors: List<ProfileVendorConfig>) : Task("preallocate_sim_profiles") {

    private val log = LoggerFactory.getLogger(javaClass)

    @Throws(Exception::class)
    override fun execute(parameters: ImmutableMultimap<String, String>, output: PrintWriter) {
        preallocateProfiles()
    }


    fun doPreprovisioning(hlrAdapter: HlrAdapter,
                          profile: String,
                          profileStats: SimInventoryDAO.SimProfileKeyStatistics) {
        val noOfProfilesToActuallyAllocate =
                Math.min(maxNoOfProfileToAllocate.toLong(), profileStats.noOfUnallocatedEntries)

        for (i in 1..noOfProfilesToActuallyAllocate) {
            val simEntry =
                    simInventoryDAO.findNextFreeSimProfileForHlr(
                            hlrId = hlrAdapter.id,
                            profile = profile)
            if (simEntry == null) {
                throw WebApplicationException("Could not find SIM profile for hlr '${hlrAdapter.name}' matching profile '${profile}'")
            }
            val simVendorAdapter = simInventoryDAO.getProfileVendorAdapterById(simEntry.profileVendorId)
            val hlrConfig = hlrConfigs.find { it.name == hlrAdapter.name }!!
            val profileVendorConfig = profileVendors.find { it.name == simVendorAdapter.name }!!

            simVendorAdapter.downloadOrder(httpClient = httpClient, dao = simInventoryDAO, simEntry = simEntry, config = profileVendorConfig)
            simVendorAdapter.confirmOrder(httpClient = httpClient, dao = simInventoryDAO, simEntry = simEntry, config = profileVendorConfig)
            hlrAdapter.activate(simEntry = simEntry, httpClient = httpClient, config = hlrConfig, dao = simInventoryDAO)
        }
    }


    /**
     * Made public to be testable.   Perform
     * allocation of profiles so that if possible, there will be tasks available for
     * provisioning.
     */
    public fun preallocateProfiles() {
        var hlrs: Collection<HlrAdapter> = simInventoryDAO.getHlrAdapters()

        for (hlr in hlrs) {
            val profiles: Collection<String> = simInventoryDAO.getProfileNamesForHlr(hlr.id)
            for (profile in profiles) {
                val profileStats =
                        simInventoryDAO.getProfileStats(hlr.id, profile)
                if (profileStats.noOfEntriesAvailableForImmediateUse < lowWaterMark) {
                    doPreprovisioning(hlrAdapter = hlr, profile = profile, profileStats = profileStats)
                }
            }
        }
    }
}