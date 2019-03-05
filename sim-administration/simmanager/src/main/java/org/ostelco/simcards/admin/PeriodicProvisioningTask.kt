package org.ostelco.simcards.admin

import com.google.common.collect.ImmutableMultimap
import io.dropwizard.servlets.tasks.Task
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.simcards.adapter.HlrEntry
import org.ostelco.simcards.adapter.HssAdapter
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
        val hlrAdapters: HlrAdapterCache,
        val profileVendors: List<ProfileVendorConfig>) : Task("preallocate_sim_profiles") {

    private val log = LoggerFactory.getLogger(javaClass)

    @Throws(Exception::class)
    override fun execute(parameters: ImmutableMultimap<String, String>, output: PrintWriter) {
        preallocateProfiles()
    }


    fun doPreprovisioning(hlrEntry: HlrEntry,
                          profile: String,
                          hlrAdapters: HlrAdapterCache,
                          profileStats: SimProfileKeyStatistics) {
        val noOfProfilesToActuallyAllocate =
                Math.min(maxNoOfProfileToAllocate.toLong(), profileStats.noOfUnallocatedEntries)

        for (i in 1..noOfProfilesToActuallyAllocate) {
            val simEntry =
                    simInventoryDAO.findNextNonProvisionedSimProfileForHlr(
                            hlrId = hlrEntry.id,
                            profile = profile)

            if (simEntry == null) {
                throw WebApplicationException("Could not find SIM profile for hlr '${hlrEntry.name}' matching profile '${profile}'")
            }

            val simVendorAdapter =
                    simInventoryDAO.getProfileVendorAdapterById(simEntry.profileVendorId)

            if (simVendorAdapter == null) {
                throw WebApplicationException("Could not find SIM vendor adapter matching id '${simEntry.profileVendorId}'")
            }

            val profileVendorConfig = profileVendors.find { it.name == simVendorAdapter.name }!!

            simVendorAdapter.downloadOrder(httpClient = httpClient, dao = simInventoryDAO, simEntry = simEntry, config = profileVendorConfig)
            simVendorAdapter.confirmOrder(httpClient = httpClient, dao = simInventoryDAO, simEntry = simEntry, config = profileVendorConfig)

            hlrAdapters.hlrAdapterById(simEntry.hlrId)!!.activate(simEntry)
        }
    }


    /**
     * Made public to be testable.   Perform
     * allocation of profiles so that if possible, there will be tasks available for
     * provisioning.
     */
    public fun preallocateProfiles() {


        for (hlr in hlrAdapters.getHlrEntries()) {
            val profiles: Collection<String> = simInventoryDAO.getProfileNamesForHlr(hlr.id)
            for (profile in profiles) {
                val profileStats =
                        simInventoryDAO.getProfileStats(hlr.id, profile)
                if (profileStats.noOfEntriesAvailableForImmediateUse < lowWaterMark) {
                    doPreprovisioning(hlrAdapters = hlrAdapters, hlrEntry = hlr, profile = profile, profileStats = profileStats)
                }
            }
        }
    }
}


class HlrAdapterCache(
        val hlrConfigs: List<HlrConfig>,
        val simInventoryDAO: SimInventoryDAO,
        val httpClient: CloseableHttpClient) {

    private val lock = Object()
    lateinit private var hlrEntries: Collection<HlrEntry>
    private val hlrAdaptersByName = mutableMapOf<String, HssAdapter>()
    private val hlrAdaptersById = mutableMapOf<Long, HssAdapter>()

    init {
       initialize()
    }

    fun getHlrEntries(): Collection<HlrEntry> {
        synchronized(lock) {
            return hlrEntries
        }
    }

    fun initialize() {
        synchronized(lock) {
            this.hlrEntries  = simInventoryDAO.getHlrEntries()
            hlrConfigs.forEach {
                val adapter = Wg2HssAdapter(httpClient, config = it, dao = simInventoryDAO)
                hlrAdaptersByName.put(it.name, adapter)
            }
        }
    }

    fun hlrAdapterByName(name: String): HssAdapter? {
        synchronized(lock) {
            return hlrAdaptersByName[name]
        }
    }

    fun hlrAdapterById(id: Long): HssAdapter? {
        synchronized(lock) {
            return hlrAdaptersById[id]
        }
    }
}