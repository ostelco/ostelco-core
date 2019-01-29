package org.ostelco.simcards.admin

import org.ostelco.simcards.smdpplus.SmDpPlusApplication
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.jdbi.DBIFactory
import io.dropwizard.testing.ConfigOverride
import org.junit.ClassRule
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Before
import org.ostelco.simcards.inventory.HlrState
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SmDpPlusState
import org.skife.jdbi.v2.DBI
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.io.FileInputStream
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType


class SimAdministrationTest {

    companion object {
        private lateinit var jdbi: DBI
        private lateinit var client: Client

        /* Emulated HLR port. */
        private var HLR_PORT = (20_000..50_000).random()

        @JvmField
        @ClassRule
        val psql = KPostgresContainer("postgres:11-alpine")
                .withInitScript("init.sql")
                .withDatabaseName("sim_manager")
                .withUsername("test")
                .withPassword("test")
                .withExposedPorts(5432)
                .waitingFor(LogMessageWaitStrategy()
                        .withRegEx(".*database system is ready to accept connections.*\\s")
                        .withTimes(2)
                        .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS)))

        init {
            psql.start()
        }

        @JvmField
        @ClassRule
        val WG2_HLR_RULE = KGenericContainer("python:3-alpine")
                .withExposedPorts(HLR_PORT)
                .withClasspathResourceMapping("wg2-hlr.py", "/service.py",
                        BindMode.READ_ONLY)
                .withEnv("PORT", "${HLR_PORT}")
                .withCommand( "python", "/service.py")

        @JvmField
        @ClassRule
        val SIM_MANAGER_RULE = DropwizardAppRule(SimAdministrationApplication::class.java,
                    ResourceHelpers.resourceFilePath("sim-manager.yaml"),
                    ConfigOverride.config("database.url", psql.jdbcUrl))

        @JvmField
        @ClassRule
        val SM_DP_PLUS_RULE = DropwizardAppRule(SmDpPlusApplication::class.java,
                ResourceHelpers.resourceFilePath("sm-dp-plus.yaml"))

        @BeforeClass
        @JvmStatic
        fun setUpDb() {
            jdbi = DBIFactory().build(SIM_MANAGER_RULE.environment,
                    SIM_MANAGER_RULE.configuration.database,
                    "db")
        }

        @BeforeClass
        @JvmStatic
        fun setUpClient() {
            client = JerseyClientBuilder(SIM_MANAGER_RULE.environment)
                    .build("test client")
        }
    }

    /* Kotlin type magic from:
       https://arnabmitra.github.io/jekyll/update/2018/01/18/TestContainers.html */
    class KPostgresContainer(imageName: String) : PostgreSQLContainer<KPostgresContainer>(imageName)
    class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

    /**
     * Set up SIM Manager DB with test data by reading the 'sample-sim-batch.csv' and
     * load the data to the DB using the SIM Manager 'import-batch' API.
     */

    val hlr = "Foo"
    val hlrProvider = "wg2"
    val profileVendor = "Bar"
    val simProfile = "FooTel_STD"

    /* ICCID with corresponding EID. To be expanded as needed.
       On changes update same table in SmDpPlus emulator (in the "SmDpPlusApplication"
       class). */
    val iccidToEidTable = mapOf(
            "8901000000000000001" to "01010101010101010101010101010101",
            "8901000000000000019" to "01010101010101010101010101010110",
            "8901000000000000027" to "01010101010101010101010101011100",
            "8901000000000000035" to "01010101010101010101010101111100"
    )

    @Before
    fun setupTables() {
        clearTables()
        presetTables()
        loadSimData()
    }

    private fun clearTables() {
        val dao = jdbi.onDemand(ClearTablesForTestingDAO::class.java)

        dao.clearTables()
    }

    private fun presetTables() {
        val dao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>().DAO

        dao.addProfileVendorAdapter(profileVendor)
        dao.addHlrAdapter(hlr, hlrProvider)
        dao.permitVendorForHlrByNames(profileVendor = profileVendor, hlr = hlr)
    }

    /* The SIM dataset is the same that is used by the SM-DP+ emulator. */
    private fun loadSimData() {
        val entries = FileInputStream(SM_DP_PLUS_RULE.configuration.simBatchData)
        val response = client.target("http://localhost:${SIM_MANAGER_RULE.getLocalPort()}/ostelco/sim-inventory/${hlr}/import-batch/profilevendor/${profileVendor}")
                .request()
                .put(Entity.entity(entries, MediaType.TEXT_PLAIN))
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun ping() {
        val response = client.target("http://localhost:${WG2_HLR_RULE.getMappedPort(HLR_PORT)}/ping")
                .request()
                .get()
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun testActivateWithHlr() {
        val apiKey = "nope"
        val payload = mapOf(
                "bssid" to hlr,
                "iccid" to "8901000000000000001",
                "msidn" to "4790000001",
                "userid" to "userid"
        )
        val response = client.target("http://localhost:${WG2_HLR_RULE.getMappedPort(HLR_PORT)}/default/provision/activate")
                .request(MediaType.APPLICATION_JSON)
                .header("x-api-key", apiKey)
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON))
        assertThat(response.status).isEqualTo(201)
    }

    @Test
    fun testGetIccid() {
        val iccid = "8901000000000000001"
        val response = client.target("http://localhost:${SIM_MANAGER_RULE.getLocalPort()}/ostelco/sim-inventory/${hlr}/iccid/${iccid}")
                .request()
                .get()
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.iccid).isEqualTo(iccid)
    }

    @Test
    fun testActivateEsim() {
        val iccid = "8901000000000000001"
        val eid = iccidToEidTable.getOrDefault(iccid, null)
        val response = client.target("http://localhost:${SIM_MANAGER_RULE.getLocalPort()}/ostelco/sim-inventory/${hlr}/esim")
                .queryParam("eid", eid)
                .queryParam("iccid", iccid)
                .request()
                .post(Entity.json(null))
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.iccid).isEqualTo(iccid)
        assertThat(simEntry.eid).isEqualTo(eid)
        assertThat(simEntry.smdpPlusState).isEqualTo(SmDpPlusState.ACTIVATED)
        assertThat(simEntry.hlrState).isEqualTo(HlrState.NOT_ACTIVATED)  /* Not yet activated in HLR. */
    }

    @Test
    fun testActivateEsimNoEid() {
        val iccid = "8901000000000000019"
        val eid = iccidToEidTable.getOrDefault(iccid, null)
        val response = client.target("http://localhost:${SIM_MANAGER_RULE.getLocalPort()}/ostelco/sim-inventory/${hlr}/esim")
                .queryParam("iccid", iccid)
                .request()
                .post(Entity.json(null))
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.iccid).isEqualTo(iccid)
        // XXX Add when 'progress' handling has been implemented
        //assertThat(simEntry.eid).isEqualTo(eid)
        assertThat(simEntry.smdpPlusState).isEqualTo(SmDpPlusState.ACTIVATED)
        assertThat(simEntry.hlrState).isEqualTo(HlrState.NOT_ACTIVATED)  /* Not yet activated in HLR. */
    }

    @Test
    fun testActivateNextEsim() {
        val iccid = "8901000000000000027"
        val eid = iccidToEidTable.getOrDefault(iccid, null)
        val response = client.target("http://localhost:${SIM_MANAGER_RULE.getLocalPort()}/ostelco/sim-inventory/${hlr}/esim")
                .queryParam("eid", eid)
                .request()
                .post(Entity.json(null))
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.eid).isEqualTo(eid)
        assertThat(simEntry.smdpPlusState).isEqualTo(SmDpPlusState.ACTIVATED)
        assertThat(simEntry.hlrState).isEqualTo(HlrState.NOT_ACTIVATED)  /* Not yet activated in HLR. */
    }
}
