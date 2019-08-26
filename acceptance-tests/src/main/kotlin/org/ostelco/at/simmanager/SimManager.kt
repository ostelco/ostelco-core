package org.ostelco.simcards.admin

import arrow.core.Either
import io.dropwizard.jdbi3.JdbiFactory
import org.jdbi.v3.core.Jdbi
import org.junit.Test
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.simcards.hss.DirectHssDispatcher
import org.ostelco.simcards.hss.HealthCheckRegistrar
import org.ostelco.simcards.hss.SimManagerToHssDispatcherAdapter
import org.ostelco.simcards.inventory.HssState
import org.ostelco.simcards.inventory.ProvisionState
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SimProfileKeyStatistics
import org.ostelco.simcards.smdpplus.SmDpPlusApplication
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy

// Trello card being used to manage development of this file: https://trello.com/c/48els9Gt

class SimManager {

    /// What we want to do.
    // Upload profiles to SM-DP+ emulator.
    // Upload profiles to HSS emulator.
    // Check that both SM-DP+ and HSS can be reached
    // Check that the healtchecks for both of these connections are accurately
    //   reflecting that the connections to HSS and SM-DP+ are working.


    // Insert profiles into Prime for an HSS without preallocated profiles.
    // Run periodic task.
    // Check that the number of available tasks is within the right range.
    // Run an API invocation via prime to allocate a profile.

    // Insert profiles into prime for an HSS with preallocated profiles
    // check that the number of available tasks is within the right range
    // Run an API invocation via prime to allocate a profile.


    /// What we may want to do
    // * Establish a DBI connection into postgres to check that the data
    //   stored there is legit.  This _could_ be used to check the statuses
    //   of the tests (both pre and postconditions).


    @Test
    fun emptyTest() {



    }
}
