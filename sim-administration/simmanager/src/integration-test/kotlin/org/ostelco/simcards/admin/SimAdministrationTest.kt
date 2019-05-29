package org.ostelco.simcards.admin

import arrow.core.Either
import com.codahale.metrics.health.HealthCheck
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


class SimAdministrationTest {

    companion object {
        private lateinit var jdbi: Jdbi
        private lateinit var client: Client

        // ICCID of first SIM in sample-sim-batch.csv
        //   ... we will be using this to check if the values for the
        //       hss state is set right.
        val FIRST_ICCID: String = "8901000000000000001"

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
    }

    private fun clearTables() {
        val dao = ClearTablesForTestingDAO(jdbi.onDemand(ClearTablesForTestingDB::class.java))

        dao.clearTables()
    }

    private fun presetTables() {
        val dao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>().getDAO()

        dao.addProfileVendorAdapter(profileVendor)
        dao.addHssEntry(hssName)
        dao.permitVendorForHssByNames(profileVendor = profileVendor, hssName = hssName)
    }

    /* The SIM dataset is the same that is used by the SM-DP+ emulator. */
    private fun loadSimData(hssState: HssState? = null, queryParameterName: String = "initialHssState", expectedReturnCode:Int = 200) {
        val entries = FileInputStream(SM_DP_PLUS_RULE.configuration.simBatchData)
        var target = client.target("$simManagerEndpoint/$hssName/import-batch/profilevendor/$profileVendor")
        if (hssState != null) {
            target = target.queryParam(queryParameterName, hssState)
        }

        val response =
                target
                .request()
                .put(Entity.entity(entries, MediaType.TEXT_PLAIN))
        assertThat(response.status).isEqualTo(expectedReturnCode)
    }

    /* TODO: SM-DP+ emuluator must be extended to support the 'getProfileStatus'
             message before this test can be enabled. */
    @Test
    @Ignore
    fun testGetProfileStatus() {
        loadSimData()
        val iccid = "8901000000000000001"
        val response = client.target("$simManagerEndpoint/$hssName/profileStatusList/$iccid")
                .request()
                .get()
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun testGetIccid() {
        loadSimData()
        val iccid = "8901000000000000001"
        val response = client.target("$simManagerEndpoint/$hssName/iccid/$iccid")
                .request()
                .get()
        assertThat(response.status).isEqualTo(200)

        val simEntry = response.readEntity(SimEntry::class.java)
        assertThat(simEntry.iccid).isEqualTo(iccid)
    }

    /* A freshly loaded DB don't have any SIM entries set
       up as a ready to use eSIM. */
    @Test
    fun testNoReadyToUseEsimAvailable() {
        loadSimData()
        val response = client.target("$simManagerEndpoint/$hssName/esim")
                .request()
                .get()
        assertThat(response.status).isEqualTo(404)
    }

    ///
    ///   Tests related to the cron job that will allocate new SIM cards
    ///   as they are required.
    ///

    @Test
    fun testGetListOfHlrs() {
        loadSimData()
        val simDao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>()
                .getDAO()
        val hssEntries = simDao.getHssEntries()

        hssEntries.mapRight {  assertEquals(1, it.size) }
        hssEntries.mapRight {  assertEquals(hssName, it[0].name) }
    }

    @Test
    fun testGetProfilesForHlr() {
        loadSimData()
        val profiles = getProfilesForHlr0()
        assertThat(profiles.isRight()).isTrue()
        profiles.map {
            assertEquals(1, it.size)
            assertEquals(expectedProfile, it[0])
        }
    }

    private fun getProfilesForHlr0(): Either<SimManagerError, List<String>> {
        val simDao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>()
                .getDAO()
        val hlrs = simDao.getHssEntries()
        assertThat(hlrs.isRight()).isTrue()

        var hlrId: Long = 0
        hlrs.map {
            hlrId = it[0].id
        }

        val profiles = simDao.getProfileNamesForHssById(hlrId)
        return profiles
    }

    @Test
    fun  testGetProfileStats() {
        loadSimData()
        val simDao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>()
                .getDAO()
        val hlrs = simDao.getHssEntries()
        assertThat(hlrs.isRight()).isTrue()

        var hlrId: Long = 0
        hlrs.map {
            hlrId = it[0].id
        }

        val stats = simDao.getProfileStats(hlrId, expectedProfile)
        assertThat(stats.isRight()).isTrue()
        stats.map {

            // The full batch is 100 numbers
            assertEquals(100L, it.noOfEntries)

            // There are 2 "golden numbers" that are marked as "reserved" and are thus
            // not allocated
            assertEquals(98L, it.noOfUnallocatedEntries)

            // But there are no released entries.
            assertEquals(0L, it.noOfReleasedEntries)
        }
    }

    @Test
    fun testPeriodicProvisioningTask() {
        loadSimData()
        val simDao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>()
                .getDAO()

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

        val dispatcher = DirectHssDispatcher(
                hssConfigs = hssConfigs,
                httpClient = httpClient,
                healthCheckRegistrar = object : HealthCheckRegistrar {
                    override fun registerHealthCheck(name: String, healthCheck: HealthCheck) {
                        SIM_MANAGER_RULE.environment.healthChecks().register(name, healthCheck)
                    }
                })
        val hssAdapterCache = SimManagerToHssDispatcherAdapter(
                dispatcher = dispatcher ,
                simInventoryDAO = simDao)
        val preStats  = SimProfileKeyStatistics(
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

        var postStats  = SimProfileKeyStatistics(0L, 0L, 0L, 0L)
        postAllocationStats.map {
            postStats = it
        }

        val noOfAllocatedProfiles =
                postStats.noOfEntriesAvailableForImmediateUse - preStats.noOfEntriesAvailableForImmediateUse
        assertEquals(
                maxNoOfProfilesToAllocate.toLong(),
                noOfAllocatedProfiles)
    }


    fun getSimEntryByICCIDFromLoadedBatch(iccid: String): SimEntry? {
        val simDao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>()
                .getDAO()
        val simProfile = simDao.getSimProfileByIccid(iccid)
        return when {
            simProfile is Either.Right -> simProfile.b
            simProfile is Either.Left ->  null
            else -> null
        }
    }

    private fun assertHssActivationOfFirstIccid(state: HssState) {
        val first = getSimEntryByICCIDFromLoadedBatch(FIRST_ICCID)
        assertNotNull(first)
        assertEquals(FIRST_ICCID, first?.iccid)
        assertEquals(state, first?.hssState)
    }

    @Test
    fun testSettingLoadedSimDataDefaultHssLoadValue() {
        loadSimData()
        assertHssActivationOfFirstIccid(HssState.NOT_ACTIVATED)
    }

    @Test
    fun testSettingLoadedSimDataToNotHaveBeenLoadedIntoHSS() {
        loadSimData(HssState.NOT_ACTIVATED)
        assertHssActivationOfFirstIccid(HssState.NOT_ACTIVATED)
    }

    @Test
    fun testSettingLoadedSimDataToHaveBeenLoadedIntoHSS() {
        loadSimData(HssState.ACTIVATED)
        assertHssActivationOfFirstIccid(HssState.ACTIVATED)
    }

    @Test
    fun badQueryParameterTest() {
        loadSimData(HssState.ACTIVATED, queryParameterName = "fooBarBaz", expectedReturnCode = 400)
    }

    private fun assertProvisioningStateOfIccid(iccid: String, state: HssState) {
        val entry = getSimEntryByICCIDFromLoadedBatch(iccid)
        assertNotNull(entry)
        assertEquals(iccid, entry?.iccid)
        assertEquals(state, entry?.hssState)
    }

    @Test
    fun testSpecialTreatmentOfGoldenNumbers() {
        loadSimData(HssState.ACTIVATED)
        // This is an ordinary MSISDN, nothing special about it, should be available
        assertEquals(ProvisionState.AVAILABLE, getSimEntryByICCIDFromLoadedBatch(FIRST_ICCID)?.provisionState)
        
        // The next two numbers are "golden", ending in respecticely "9999" and "0000", so they
        // should be reserved, and thus not available.
        assertEquals(ProvisionState.RESERVED, getSimEntryByICCIDFromLoadedBatch("8901000000000000985")?.provisionState)
        assertEquals(ProvisionState.RESERVED, getSimEntryByICCIDFromLoadedBatch("8901000000000000993")?.provisionState)
    }
}
