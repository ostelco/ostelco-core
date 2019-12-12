package org.ostelco.simcards.admin

import arrow.core.Either
import com.google.common.collect.ImmutableMultimap
import io.dropwizard.servlets.tasks.Task
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.sim.es2plus.ProfileStatus
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SmDpPlusState
import org.ostelco.simcards.profilevendors.ProfileVendorAdapterFactory
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

class PollOutstandingProfilesTask(
        val simInventoryDAO: SimInventoryDAO,
        val pvaf: ProfileVendorAdapterFactory) : Task("poll_outstanding_profiles") {

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

    private fun pollAllocatedButNotDownloadedProfiles(output: PrintWriter): Either<SimManagerError, Unit> {
        return simInventoryDAO.findAllocatedButNotDownloadedProfiles().map { profilesToPoll ->
            pollForSmdpStatus(output, profilesToPoll)
        }
    }

    private fun pollForSmdpStatus(output: PrintWriter, profilesToPoll: List<SimEntry>) {

        val result = StringBuffer()

        @Synchronized
        fun reportln(s: String, e: Exception? = null) {
            output.println(s)
            if (e != null) {
                output.println("   Caused exception: $e")
            }
        }

        fun sendReport() {
            output.print(result.toString())
        }

        fun updateProfileInDb(p: ProfileStatus) {
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
                        val report = "Updated  state for iccid=$iccid to ${state.name}"
                        logger.info(report)
                        reportln(report)
                    }
                } else {
                    reportln("State for iccid=$iccid still set to ${state.name}")
                }
            } catch (e: Exception) {
                reportln("Couldn't update status for iccid ${p.iccid}", e)
            }
        }


        fun asVendorIdToIccdList(profiles: List<SimEntry>): Map<Long, List<String>> =
                profiles.map {
                    mapOf(it.profileVendorId to it.iccid)
                }.flatMap {
                    it.entries
                }.groupBy {
                    it.key
                }.mapValues { vendor ->
                    vendor.value.map { it.value }
                }
        

        // Then poll them individually via the profile vendor adapter associated with the
        // profile (there is not much to be gained here by doing it in paralellel, but perhaps
        // one day it will be).

        for ((vendorId, iccidList) in asVendorIdToIccdList(profilesToPoll)) {
            pvaf.getAdapterByVendorId(vendorId).mapRight { profileVendorAdapter ->
                val statuses =
                        profileVendorAdapter.getProfileStatusList(iccidList)
                statuses.mapRight {
                    it.forEach { updateProfileInDb(it) }
                }
            }  // TODO: Am I missing the error situation here?
        }
    }
}

