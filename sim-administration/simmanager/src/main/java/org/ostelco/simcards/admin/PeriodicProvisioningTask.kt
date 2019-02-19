package org.ostelco.simcards.admin

import com.google.common.collect.ImmutableMultimap
import io.dropwizard.servlets.tasks.Task
import org.ostelco.simcards.adapter.HlrAdapter
import org.ostelco.simcards.inventory.SimInventoryDAO
import java.io.PrintWriter




class PreallocateProfilesTask(val simInventoryDAO: SimInventoryDAO) : Task("preallocate_sim_profiles") {

    @Throws(Exception::class)
    override fun execute(parameters: ImmutableMultimap<String, String>, output: PrintWriter) {

        preallocateProfiles()
    }

    /**
     * Made public to be testable.   Perform
     * allocation of profiles so that if possible, there will be tasks available for
     * provisioning.
     */
    fun preallocateProfiles() {
        var hlrs: Collection<HlrAdapter> = simInventoryDAO.getHlrAdapters()

        for (hlr in hlrs) {
            val profiles: Collection<String> = simInventoryDAO.getProfileNamesForHlr(hlr.id)
            for (profile in profiles) {
                val profileStats = simInventoryDAO.getProfileStats(hlr.id, profile)

                // XXX TODO regulate & log based on profileStats.

            }
        }
    }
}