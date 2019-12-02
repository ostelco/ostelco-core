package org.ostelco.simcards.admin

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.google.common.collect.ImmutableMultimap
import io.dropwizard.servlets.tasks.Task
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.simmanager.AdapterError
import org.ostelco.prime.simmanager.DatabaseError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.prime.simmanager.SystemError
import org.ostelco.simcards.hss.HssEntry
import org.ostelco.simcards.hss.SimManagerToHssDispatcherAdapter
import org.ostelco.simcards.inventory.HssState
import org.ostelco.simcards.inventory.ProvisionState
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimInventoryDAO
import org.ostelco.simcards.inventory.SimProfileKeyStatistics
import org.ostelco.simcards.profilevendors.ProfileVendorAdapterFactory
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
 *
 * TODO:  This is so incredibly complicated, and there is no need for it to be.
 *        Just fix it!
 */

class PreallocateProfilesTask(
        private val lowWaterMark: Int = 10,
        val maxNoOfProfileToAllocate: Int = 30,
        val pvaf : ProfileVendorAdapterFactory,
        val simInventoryDAO: SimInventoryDAO,
        val hssAdapterProxy: SimManagerToHssDispatcherAdapter) : Task("preallocate_sim_profiles") {

    private val logger by getLogger()

    @Throws(Exception::class)
    override fun execute(parameters: ImmutableMultimap<String, String>, output: PrintWriter) {
        // TODO: Rewrite to deliver a report of what happened, also if things went well.
        preAllocateSimProfiles()
                .mapLeft { simManagerError ->
                    logger.error(simManagerError.description)
                    output.println(asJson(simManagerError))
                }
    }

    // TODO: This method must be refactored. It is still _way_ too complex.
    private fun preProvisionSimProfile(hssEntry: HssEntry,
                                       simEntry: SimEntry): Either<SimManagerError, SimEntry> =
            if (simEntry.id == null) { // TODO: This idiom is _bad_, find something better!
                AdapterError("simEntry.id == null for simEntry = '$simEntry'.").left()
            } else
                pvaf.getAdapterByVendorId(simEntry.profileVendorId)
                        .flatMap { profileVendorAdapter ->
                            when {
                                simEntry.hssState == HssState.NOT_ACTIVATED -> {
                                    logger.debug("Preallocating (HSS not activated) for HSS with ID/metricName ${hssEntry.id}/${hssEntry.name} simEntry with ICCID=${simEntry.iccid}")

                                    profileVendorAdapter.activate(simEntry = simEntry)
                                            .flatMap {
                                                hssAdapterProxy.activate(simEntry)
                                            }
                                            .flatMap {
                                                simInventoryDAO.setHssState(simEntry.id, HssState.ACTIVATED)
                                            }

                                }
                                else -> {
                                    // TODO: THis looks like  bug! It looks like the preallocation will _either_ run against the HSS, _or_ against the profile vendor adapter.
                                    //       This is clearly wrong, it should run against both.
                                    logger.debug("Preallocating (HSS preactivated) for HSS with ID/metricName ${hssEntry.id}/${hssEntry.name} simEntry with ICCID=${simEntry.iccid}")
                                    profileVendorAdapter.activate(simEntry = simEntry)
                                }
                            }
                        }


    private fun batchPreprovisionSimProfiles(hssEntry: HssEntry,
                                             simProfileName: String,
                                             profileStats: SimProfileKeyStatistics): Either<SimManagerError, Any> {

        logger.debug("preprovisioning hssEntry='$hssEntry', simProfileName='$simProfileName', profileStats='$profileStats.'")

        val noOfProfilesToActuallyAllocate =
                min(maxNoOfProfileToAllocate.toLong(), profileStats.noOfUnallocatedEntries)

        if (noOfProfilesToActuallyAllocate == 0L) {
            logger.error("Could not find any profiles to allocate for hssname = '{}', profilename = '{}', profileStats = '{}'",
                    hssEntry.name,
                    simProfileName,
                    profileStats)
        } else
            for (i in 1..noOfProfilesToActuallyAllocate) {
                logger.debug("preprovisioning for profileName='$simProfileName', HSS with ID/metricName ${hssEntry.id}/${hssEntry.name}. Iteration index = $i")
                val focus = simInventoryDAO.findNextNonProvisionedSimProfileForHss(hssId = hssEntry.id, profile = simProfileName)
                focus.mapLeft { error -> return error.left() }
                focus.flatMap { simEntry -> preProvisionSimProfile(simEntry, simProfileName, hssEntry).right() }
            }

        // TODO: Replace the text "Unit" with a report of what was actually done by the provisioning
        //       task (eventually this will be the report collected by a report builder that has yet to be constructed)
        return Unit.right()
    }

    private fun preProvisionSimProfile(simEntry: SimEntry, simProfileName: String, hssEntry: HssEntry): Either<SimManagerError, Any> {
        logger.debug("preprovisioning for profileName='$simProfileName', HSS with ID/metricName ${hssEntry.id}/${hssEntry.name} simEntry with ICCID=${simEntry.iccid}, id = ${simEntry.id}")
        return if (simEntry.id == null) {
            DatabaseError("This should never happen, since everything that is read from a database should have an ID")
                    .left()
        } else {
            preProvisionSimProfile(hssEntry, simEntry)
                    .mapLeft { simManagerError ->
                        logger.error("Preallocation of SIM ICCID {} failed with error: {}}",
                                simEntry.iccid, simManagerError.description)
                        simInventoryDAO.setProvisionState(simEntry.id, ProvisionState.ALLOCATION_FAILED)
                        simManagerError
                    }
        }
    }

    fun preAllocateSimProfiles(): Either<SimManagerError, Unit> =
            simInventoryDAO.getHssEntries().flatMap { entries ->
                entries.forEach {
                    preAllocateSimProfilesForHss(it)
                }.right()
            }

    private fun preAllocateSimProfilesForHss(hssEntry: HssEntry): Either<SimManagerError, Unit> {
        logger.run { debug("Start of prealloacation for HSS with ID/metricName ${hssEntry.id}/${hssEntry.name}") }
        return simInventoryDAO.getProfileNamesForHssById(hssEntry.id).flatMap { simProfileNames ->
            simProfileNames.forEach {
                preAllocateSimProfileForHss(hssEntry, it)
            }.right()
        }
    }

    private fun preAllocateSimProfileForHss(hssEntry: HssEntry, simProfileName: String): Either<SimManagerError, Unit> {
        val thisProfilesNameForLogging = "profiles  where  ID/metricName/simProfileName=${hssEntry.id}/${hssEntry.name}/$simProfileName"
        logger.debug("Start of prealloacation for $thisProfilesNameForLogging")
        return simInventoryDAO.getProfileStats(hssEntry.id, simProfileName).flatMap { profileStats ->
            if (profileStats.noOfUnallocatedEntries == 0L) {
                val msg = "No  more unallocated  $thisProfilesNameForLogging"
                logger.error(msg)
                return SystemError(msg).left()
            } else {
                logger.debug("Ready for use: $thisProfilesNameForLogging = ${profileStats.noOfEntriesAvailableForImmediateUse}")
                if (profileStats.noOfEntriesAvailableForImmediateUse < lowWaterMark) {
                    logger.info("Preallocating new SIM batch for $thisProfilesNameForLogging",
                            hssEntry.name, simProfileName)
                    batchPreprovisionSimProfiles(hssEntry = hssEntry, simProfileName = simProfileName, profileStats = profileStats)
                }
                // To satisfy  Arrow we must all unite, right?, ....
                Unit.right()
            }
        }
    }
}

