package org.ostelco.simcards.admin

import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.jdbi3.JdbiFactory
import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.assertj.core.api.Assertions.assertThat
import org.glassfish.jersey.client.ClientProperties
import org.jdbi.v3.core.Jdbi
import org.junit.*
import org.junit.Assert.assertEquals
import org.ostelco.simcards.hss.HssProxy
import org.ostelco.simcards.hss.mapRight
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


class SimAdministrationTest {

    companion object {
        private lateinit var jdbi: Jdbi
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
            jdbi = JdbiFactory()
                    .build(SIM_MANAGER_RULE.environment, SIM_MANAGER_RULE.configuration.database,
                            "db")
                    .installPlugins()
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
    class KPostgresContainer(imageName: String) :
            PostgreSQLContainer<KPostgresContainer>(imageName)

    class KFixedHostPortGenericContainer(imageName: String) :
            FixedHostPortGenericContainer<KFixedHostPortGenericContainer>(imageName)

    private val hssName = "Foo"
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
    fun setUp() {
        SM_DP_PLUS_RULE.getApplication<SmDpPlusApplication>().reset()
        clearTables()
        presetTables()
        loadSimData()
    }

    private fun clearTables() {
        val dao = ClearTablesForTestingDAO(jdbi.onDemand(ClearTablesForTestingDB::class.java))

        dao.clearTables()
    }

    private fun presetTables() {
        val dao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>().DAO

        dao.addProfileVendorAdapter(profileVendor)
        dao.addHssEntry(hssName)
        dao.permitVendorForHssByNames(profileVendor = profileVendor, hssName = hssName)
    }

    /* The SIM dataset is the same that is used by the SM-DP+ emulator. */
    private fun loadSimData() {
        val entries = FileInputStream(SM_DP_PLUS_RULE.configuration.simBatchData)
        val response = client.target("$simManagerEndpoint/$hssName/import-batch/profilevendor/$profileVendor")
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
        val response = client.target("$simManagerEndpoint/$hssName/profileStatusList/$iccid")
                .request()
                .get()
        assertThat(response.status).isEqualTo(200)
    }




    @Test
    fun testGetIccid() {
        val iccid = "8901000000000000001"
        val response = client.target("$simManagerEndpoint/$hssName/iccid/$iccid")
                .request()
                .get()
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.iccid).isEqualTo(iccid)
    }


    ///
    ///   Tests related to the cron job that will allocate new SIM cards
    ///   as they are required.
    ///

    @Test
    fun testGetListOfHlrs() {
        val simDao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>().DAO

        val hssEntries = simDao.getHssEntries()
        hssEntries.mapRight {  assertEquals(1, it.size) }

        hssEntries.mapRight {  assertEquals(hssName, it[0].name)}

    }

    @Test
    fun testGetProfilesForHlr() {
        val simDao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>()
                .DAO
        val hlrs = simDao.getHssEntries()
        assertThat(hlrs.isRight()).isTrue()

        var hlrId: Long = 0
        hlrs.map {
            hlrId = it[0].id
        }

        val profiles = simDao.getProfileNamesForHssById(hlrId)
        assertThat(profiles.isRight()).isTrue()
        profiles.map {
            assertEquals(1, it.size)
            assertEquals(expectedProfile, it.get(0))
        }
    }

    @Test
    fun  testGetProfileStats() {
        val simDao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>()
                .DAO
        val hlrs = simDao.getHssEntries()
        assertThat(hlrs.isRight()).isTrue()

        var hlrId: Long = 0
        hlrs.map {
            hlrId = it[0].id
        }

        val stats = simDao.getProfileStats(hlrId, expectedProfile)
        assertThat(stats.isRight()).isTrue()
        stats.map {
            assertEquals(100L, it.noOfEntries)
            assertEquals(100L, it.noOfUnallocatedEntries)
            assertEquals(0L, it.noOfReleasedEntries)
        }
    }

    @Test
    fun testPeriodicProvisioningTask() {

        val simDao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>()
                .DAO

        val profileVendors = SIM_MANAGER_RULE.configuration.profileVendors
        val hssConfigs = SIM_MANAGER_RULE.configuration.hssVendors
        val httpClient = HttpClientBuilder(SIM_MANAGER_RULE.environment)
                .build("periodicProvisioningTaskClient")
        val maxNoOfProfilesToAllocate = 10

        val hlrs = simDao.getHssEntries()
        assertThat(hlrs.isRight()).isTrue()

        var hssId: Long = 0
        hlrs.map {
            hssId = it[0].id
        }

        val hssAdapterCache = HssProxy(
                hssConfigs = hssConfigs,
                simInventoryDAO = simDao,
                httpClient = httpClient)

        var preStats: SimProfileKeyStatistics =
                SimProfileKeyStatistics(
                        0L,
                        0L,
                        0L,
                        0L)

        val task = PreallocateProfilesTask(
                profileVendors = profileVendors,
                simInventoryDAO = simDao,
                maxNoOfProfileToAllocate = maxNoOfProfilesToAllocate,
                hssAdapterProxy = hssAdapterCache,
                httpClient = httpClient)

        task.preAllocateSimProfiles()

        val postAllocationStats =
                simDao.getProfileStats(hssId, expectedProfile)
        assertThat(postAllocationStats.isRight()).isTrue()
        var postStats: SimProfileKeyStatistics = SimProfileKeyStatistics(0L, 0L, 0L, 0L)
        postAllocationStats.map {
            postStats = it
        }

        val noOfAllocatedProfiles =
                postStats.noOfEntriesAvailableForImmediateUse - preStats.noOfEntriesAvailableForImmediateUse

        assertEquals(
                maxNoOfProfilesToAllocate.toLong(),
                noOfAllocatedProfiles)
    }
}
