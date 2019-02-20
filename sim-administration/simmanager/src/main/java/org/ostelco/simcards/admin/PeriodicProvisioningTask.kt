package org.ostelco.simcards.admin

import com.google.common.collect.ImmutableMultimap
import io.dropwizard.servlets.tasks.Task
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.sim.es2plus.ES2PlusClient
import org.ostelco.sim.es2plus.Es2DownloadOrderResponse
import org.ostelco.sim.es2plus.FunctionExecutionStatusType
import org.ostelco.simcards.adapter.HlrAdapter
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SmDpPlusState
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
        val es2PlusClient: ES2PlusClient,
        val httpClient: CloseableHttpClient,
        val hlrConfigs: List<HlrConfig>) : Task("preallocate_sim_profiles") {

    private val log = LoggerFactory.getLogger(javaClass)

    @Throws(Exception::class)
    override fun execute(parameters: ImmutableMultimap<String, String>, output: PrintWriter) {
        preallocateProfiles()
    }


    // TODO: Must be a transaction, and must also operate exclusively on a single
    //       ICCID from start to finish, no race conditions permitted!
    fun preprovision(simEntry: SimEntry, hlrAdapter: HlrAdapter): Boolean {

        val hlrConfig = hlrConfigs.find { it.name == hlrAdapter.name }

        if (hlrConfig == null) {
            return false
        }

        val downloadResponse: Es2DownloadOrderResponse =
                es2PlusClient.downloadOrder(iccid = simEntry.iccid)

        if (FunctionExecutionStatusType.ExecutedSuccess != downloadResponse.header.functionExecutionStatus.status) {
            return false// TODO:  Bad things happened Clean up, make sure that the database is consistent, then abort
        }

        simInventoryDAO.setSmDpPlusState(simEntry.id!!, SmDpPlusState.ALLOCATED)


        val confirmOrderResponse =
                es2PlusClient.confirmOrder(iccid = simEntry.iccid, releaseFlag = true)

        if (FunctionExecutionStatusType.ExecutedSuccess != confirmOrderResponse.header.functionExecutionStatus.status) {
            return false // TODO:  Bad things happened Clean up, make sure that the database is consistent, then abort
        }

        // At this point we have allocated everything there is to allocate in the SM-DP+, so we proceed to the
        // HSS.

        // XXX This method will throw a WebApplicationException exception on error, but will also update all
        //     relevant fields in the dao.  Catch the error and return an appropriate response.
        try {
            hlrAdapter.activate(simEntry = simEntry, httpClient = httpClient, config = hlrConfig, dao = simInventoryDAO)
        } catch (e: WebApplicationException) {
            return false  // On error, but state information is actually set right at this time
        }
        return true
    }


    fun doPreprovisioning(hlrAdapter: HlrAdapter,
                          profile: String,
                          profileStats: SimInventoryDAO.SimProfileKeyStatistics) : Boolean{
        val noOfProfilesToActuallyAllocate =
                Math.min(maxNoOfProfileToAllocate.toLong(), profileStats.noOfUnallocatedEntries)

        for (i in 1 .. noOfProfilesToActuallyAllocate) {
            val simEntry =
                    simInventoryDAO.findNextFreeSimProfileForHlr(
                            hlrId = hlrAdapter.id,
                            profile = profile)
            if (simEntry != null) {
                if (preprovision(simEntry = simEntry, hlrAdapter =  hlrAdapter)) {
                    log.error("Failed to preprovision simEntry = $simEntry")
                    return false
                }
            } else {
                log.info("Could not find any free SIM entry to preprovision")
            }
        }
        return true
    }



    /**
     * Made public to be testable.   Perform
     * allocation of profiles so that if possible, there will be tasks available for
     * provisioning.
     */
    fun preallocateProfiles() : Boolean {
        var hlrs: Collection<HlrAdapter> = simInventoryDAO.getHlrAdapters()

        for (hlr in hlrs) {
            val profiles: Collection<String> = simInventoryDAO.getProfileNamesForHlr(hlr.id)
            for (profile in profiles) {
                val profileStats =
                        simInventoryDAO.getProfileStats(hlr.id, profile)
                if (profileStats.noOfEntriesAvailableForImmediateUse < lowWaterMark) {
                    if (! doPreprovisioning(hlrAdapter= hlr, profile = profile, profileStats = profileStats)) {
                        return false
                    }
                }
            }
        }
        return true
    }
}