package org.ostelco.simcards.admin

import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.jdbi.DBIFactory
import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.assertj.core.api.Assertions.assertThat
import org.glassfish.jersey.client.ClientProperties
import org.junit.*
import org.ostelco.simcards.inventory.HlrState
import org.ostelco.simcards.inventory.SimEntry
import org.ostelco.simcards.inventory.SmDpPlusState
import org.ostelco.simcards.smdpplus.SmDpPlusApplication
import org.skife.jdbi.v2.DBI
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


class SimAdministrationTest {

    companion object {
        private lateinit var jdbi: DBI
        private lateinit var client: Client

        /* Port number exposed to host by the emulated HLR service. */
        private var HLR_PORT = (20_000..29_999).random()

        @JvmField
        @ClassRule
        val psql: KPostgresContainer = KPostgresContainer("postgres:11-alpine")
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
        val SM_DP_PLUS_RULE = DropwizardAppRule(SmDpPlusApplication::class.java,
                ResourceHelpers.resourceFilePath("sm-dp-plus.yaml"))

        @JvmField
        @ClassRule
        val HLR_RULE: KFixedHostPortGenericContainer = KFixedHostPortGenericContainer("python:3-alpine")
                .withFixedExposedPort(HLR_PORT, 8080)
                .withExposedPorts(8080)
                .withClasspathResourceMapping("hlr.py", "/service.py",
                        BindMode.READ_ONLY)
                .withCommand( "python", "/service.py")

        @JvmField
        @ClassRule
        val SIM_MANAGER_RULE = DropwizardAppRule(SimAdministrationApplication::class.java,
                    ResourceHelpers.resourceFilePath("sim-manager.yaml"),
                    ConfigOverride.config("database.url", psql.jdbcUrl),
                    ConfigOverride.config("hlrs[0].endpoint", "http://localhost:$HLR_PORT/default/provision"))

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
                    .withProperty(ClientProperties.READ_TIMEOUT, 5000)
                    .build("test client")
        }
    }

    /* Kotlin type magic from:
       https://arnabmitra.github.io/jekyll/update/2018/01/18/TestContainers.html */
    class KPostgresContainer(imageName: String) : PostgreSQLContainer<KPostgresContainer>(imageName)
    class KFixedHostPortGenericContainer(imageName: String) : FixedHostPortGenericContainer<KFixedHostPortGenericContainer>(imageName)

    private val hlr = "Foo"
    private val profileVendor = "Bar"
    private val phoneType = "rababara"
    private val expectedProfile = "IPHONE_PROFILE_2"

    /* Test endpoint. */
    private val simManagerEndpoint = "http://localhost:${SIM_MANAGER_RULE.localPort}/ostelco/sim-inventory"

    /* Generate a fixed corresponding EID based on ICCID.
       Same code is used in SM-DP+ emulator. */
    private fun getEidFromIccid(iccid: String): String? = if (iccid.isNotEmpty())
        "01010101010101010101" + iccid.takeLast(12)
    else
        null

    /**
     * Set up SIM Manager DB with test data by reading the 'sample-sim-batch.csv' and
     * load the data to the DB using the SIM Manager 'import-batch' API.
     */

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
        dao.addHlrAdapter(hlr)
        dao.permitVendorForHlrByNames(profileVendor = profileVendor, hlr = hlr)
    }

    /* The SIM dataset is the same that is used by the SM-DP+ emulator. */
    private fun loadSimData() {
        val entries = FileInputStream(SM_DP_PLUS_RULE.configuration.simBatchData)
        val response = client.target("$simManagerEndpoint/$hlr/import-batch/profilevendor/$profileVendor")
                .request()
                .put(Entity.entity(entries, MediaType.TEXT_PLAIN))
        assertThat(response.status).isEqualTo(200)
    }

    /* XXX SM-DP+ emuluator must be extended to support the 'getProfileStatus'
       message before this test can be enabled. */
    @Test
    @Ignore
    fun testGetProfileStatus() {
        val iccid = "8901000000000000001"
        val response = client.target("$simManagerEndpoint/$hlr/profileStatusList/$iccid")
                .request()
                .get()
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun testActivateWithHlr() {
        val iccid = "8901000000000000001"
        val response = client.target("$simManagerEndpoint/$hlr/iccid/$iccid")
                .request()
                .post(Entity.json(null))
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.iccid).isEqualTo(iccid)
        assertThat(simEntry.hlrState).isEqualTo(HlrState.ACTIVATED)
    }

    @Test
    fun testDeactivateWithHlr() {
        val iccid = "8901000000000000001"
        val response = client.target("$simManagerEndpoint/$hlr/iccid/$iccid")
                .request()
                .delete()
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.iccid).isEqualTo(iccid)
        assertThat(simEntry.hlrState).isEqualTo(HlrState.NOT_ACTIVATED)
    }

    @Test
    fun testGetIccid() {
        val iccid = "8901000000000000001"
        val response = client.target("$simManagerEndpoint/$hlr/iccid/$iccid")
                .request()
                .get()
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.iccid).isEqualTo(iccid)
    }

    @Test
    fun testActivateEsim() {
        val iccid = "8901000000000000001"
        val eid = getEidFromIccid(iccid)
        val response = client.target("$simManagerEndpoint/$hlr/esim")
                .queryParam("eid", eid)
                .queryParam("iccid", iccid)
                .request()
                .post(Entity.json(null))
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.iccid).isEqualTo(iccid)
        assertThat(simEntry.eid).isEqualTo(eid)
        assertThat(simEntry.profile).isEqualTo(expectedProfile)
        assertThat(simEntry.smdpPlusState).isEqualTo(SmDpPlusState.RELEASED)
        assertThat(simEntry.hlrState).isEqualTo(HlrState.NOT_ACTIVATED)
    }

    @Test
    fun testActivateEsimNoEid() {
        val iccid = "8901000000000000019"
        val response = client.target("$simManagerEndpoint/$hlr/esim")
                .queryParam("iccid", iccid)
                .request()
                .post(Entity.json(null))
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.iccid).isEqualTo(iccid)
        assertThat(simEntry.eid).isEqualTo(getEidFromIccid(iccid))
        assertThat(simEntry.profile).isEqualTo(expectedProfile)
        assertThat(simEntry.smdpPlusState).isEqualTo(SmDpPlusState.RELEASED)
        assertThat(simEntry.hlrState).isEqualTo(HlrState.NOT_ACTIVATED)
    }

    @Test
    fun testActivateNextEsim() {
        val iccid = "8901000000000000027"
        val eid = getEidFromIccid(iccid)
        val response = client.target("$simManagerEndpoint/$hlr/esim")
                .queryParam("eid", eid)
                .request()
                .post(Entity.json(null))
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.eid).isEqualTo(eid)
        assertThat(simEntry.profile).isEqualTo(expectedProfile)
        assertThat(simEntry.smdpPlusState).isEqualTo(SmDpPlusState.RELEASED)
        assertThat(simEntry.hlrState).isEqualTo(HlrState.NOT_ACTIVATED)
    }

    @Test
    fun testActivateEsimNoEidAll() {
        val iccid = "8901000000000000035"
        val response = client.target("$simManagerEndpoint/$hlr/esim/all")
                .queryParam("iccid", iccid)
                .request()
                .post(Entity.json(null))
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.iccid).isEqualTo(iccid)
        assertThat(simEntry.eid).isEqualTo(getEidFromIccid(iccid))
        assertThat(simEntry.profile).isEqualTo(expectedProfile)
        assertThat(simEntry.smdpPlusState).isEqualTo(SmDpPlusState.RELEASED)
        assertThat(simEntry.hlrState).isEqualTo(HlrState.ACTIVATED)
    }

    /* XXX Fails due to DB being reset after each test. */
    @Test
    @Ignore
    fun testActivateNextEsimNoEidAll() {
        val response = client.target("$simManagerEndpoint/$hlr/esim/all")
                .request()
                .post(Entity.json(null))
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.eid).isEqualTo(getEidFromIccid(simEntry.iccid))
        assertThat(simEntry.profile).isEqualTo(expectedProfile)
        assertThat(simEntry.smdpPlusState).isEqualTo(SmDpPlusState.RELEASED)
        assertThat(simEntry.hlrState).isEqualTo(HlrState.ACTIVATED)
    }




    @Test
    fun testGetListOfHlrs() {
        val simDao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>().DAO

        val hlrs = simDao.getListOfHLRs()
        assertEquals(1,hlrs.size)
        assertTrue(hlrs.contains("Foo"))
    }
}
