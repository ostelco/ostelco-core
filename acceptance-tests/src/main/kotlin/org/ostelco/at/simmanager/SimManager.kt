package org.ostelco.simcards.admin

import arrow.core.Either
import com.codahale.metrics.health.HealthCheck
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.jdbi3.JdbiFactory
import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.assertj.core.api.Assertions.assertThat
import org.glassfish.jersey.client.ClientProperties
import org.jdbi.v3.core.Jdbi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Ignore
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
import java.io.FileInputStream
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType


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
