package org.ostelco.simcards.admin

import com.google.common.collect.ImmutableMultimap
import io.dropwizard.servlets.tasks.Task
import java.io.PrintWriter


class PreallocateProfiles() : Task("preallocate_sim_profiles") {

    @Throws(Exception::class)
    override fun execute(parameters: ImmutableMultimap<String, String>, output: PrintWriter) {
        // Do stuff here
    }
}