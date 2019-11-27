package org.ostelco.simcards.admin

import arrow.core.Either
import com.google.common.collect.ImmutableMultimap
import io.dropwizard.servlets.tasks.Task
import kotlinx.coroutines.newFixedThreadPoolContext
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.SimProfile
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.sim.es2plus.ES2PlusClient
import org.ostelco.sim.es2plus.ProfileStatus
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SmDpPlusState
import java.io.PrintWriter


/**
 * A dropwizard "task" that is intended to be invoked as an administrative step
 * by an external agent that is part of the serving system, not a customer of it.
 *
 * The task implements a task that for all of the sim profiles in storage
 * will search for profiles that has been allocated to end user equipment
 * but has not yet been downloaded or installed by it, as seen by ES2+
 * callbacks.
 *
 * The functionality is duplicating some of the functions implemented by
 * the ES2+ callback mechanism. The reason why we need this task in addition
 * is that we have found the ES2+ callback to be unreliable.  This task
 * is therefore intended to serve as a fallback for that when working correctly
 * preferable mechanism.
 */

class PollOutstandingProfiles(
        val simInventoryDAO: SimInventoryDAO,
        val client: ES2PlusClient,
        val profileVendors: List<ProfileVendorConfig>) : Task("poll_outstanding_profiles") {


    private val logger by getLogger()


    @Throws(Exception::class)
    override fun execute(parameters: ImmutableMultimap<String, String>, output: PrintWriter) {
        // TODO: Rewrite to deliver a report of what happened, also if things went well.
        pollAllocatedButNotDownloadedProfiles(output)
                .mapLeft { simManagerError ->
                    logger.error(simManagerError.description)
                    output.println(asJson(simManagerError))
                }
    }


    private fun pollAllocatedButNotDownloadedProfiles(output: PrintWriter): Either<SimManagerError, Unit> =
            simInventoryDAO.getAllocatedButNotDownloadedProfiles().flatMap { profilesToPoll ->
                pollForSmdpStatus(output, profilesToPoll)
            }.right()

    private fun pollForSmdpStatus(output: PrintWriter, profilesToPoll: List<SimProfile>) {

        val result = StringBuffer()

        @Synchronized
        fun reportln(s: String) {
            output.println(s)
        }

        fun sendReport() {
            output.print(result.toString())
        }

        fun pollProfile(p: ProfileStatus) {
            try {

                // Get state and iccid non null strings
                val stateString = p.state ?: ""
                val iccid = p.iccid ?: ""

                // Then discard empty states.
                if (stateString == "") {
                    reportln("Empty state detected for iccid ${p.iccid}")
                    return
                }

                val state = SmDpPlusState.valueOf(stateString.toUpperCase())

                if (state != SmDpPlusState.RELEASED) {
                    val update = simInventoryDAO.setSmDpPlusStateUsingIccid(iccid, state)
                    if (update.isLeft()) {  // TODO: This is is not idiomatic. Please help me make it idomatic before merging to develop.
                        update.mapLeft {
                            reportln("Could not update  iccid=$iccid still set to ${state.name}. Sim manager error = $it")
                        }
                    } else {
                        // This probably represents an error situation in the SM-DP+, and _should_ perhaps
                        // be reported as an error by the sim manager. Please think about this and
                        // either change it to logger.error yourself, or ask me in review comments to do it for you.
                        logger.info("Updated  state for iccid=$iccid still set to ${state.name}")
                        reportln("Updated  state for iccid=$iccid still set to ${state.name}")
                    }
                } else {
                    reportln("Just state for iccid=$iccid still set to ${state.name}")
                }

            } catch (e: Exception) {
                reportln("Couldn't fetch status for iccid ${p.iccid}", e)
            }
        }

        fun pollProfiles(statusList: List<ProfileStatus>) {
            // TODO: Rewrite this so that it runs up to 100 queries in parallell
            // ES2+ is slow, so som paralellism is useful.  Shouldn't be too much, since we
            // might otherwise run out of file descriptors.
            for (p in statusList) {
                pollProfile(p)
            }
        }

        // First get the profile statuses
        val statuses = client.profileStatus(profilesToPoll.map { it.iccId })
        val statusList = statuses.profileStatusList

        // Handle the error situation that we got a null response by reporting it both to
        // the logger and the caller.
        if (statusList == null) {
            logger.error("Could not get statuses from SMDP+ for ")
            reportln("Got null status list for iccid list $iccids")
            return // TODO: Log som error situation first, also write to the response thingy
        }

        // Happy day scenario.  Update statuses, report results.
        pollProfiles(statusList)
    }
}

