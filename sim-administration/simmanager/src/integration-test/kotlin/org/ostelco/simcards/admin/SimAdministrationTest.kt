package org.ostelco.simcards.admin

import org.ostelco.simcards.smdpplus.SmDpPlusApplication
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.jdbi.DBIFactory
import org.junit.ClassRule
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Before
import org.ostelco.simcards.inventory.SimInventoryCreationDestructionDAO
import org.skife.jdbi.v2.DBI
import java.io.FileInputStream
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType

class SimAdministrationTest {

    companion object {
        private lateinit var jdbi: DBI
        private lateinit var client: Client

        @JvmField
        @ClassRule
        val SIM_MANAGER_RULE = DropwizardAppRule(SimAdministrationApplication::class.java,
                ResourceHelpers.resourceFilePath("sim-manager.yaml"))

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

    /**
     * Set up SIM Manager DB with test data by reading the 'sample-sim-batch.csv' and
     * load the data to the DB using the SIM Manager 'import-batch' API.
     */

    val hlr = "Foo"
    val profileVendor = "Bar"
    val simProfile = "FooTel_STD"

    @Before
    fun createTables() {
        resetTables()
        presetTables()
        loadSimData()
    }

    private fun resetTables() {
        val dao = jdbi.onDemand(SimInventoryCreationDestructionDAO::class.java)

        dao.dropAll()
        dao.createAll()
    }

    private fun presetTables() {
        val dao = SIM_MANAGER_RULE.getApplication<SimAdministrationApplication>().simInventoryDAO

        dao.addProfileVendorAdapter(profileVendor)
        dao.addHlrAdapter(hlr)
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
    fun testGetIccid() {
        val iccid = "8901000000000000001"
        val response = client.target("http://localhost:${SIM_MANAGER_RULE.getLocalPort()}/ostelco/sim-inventory/${hlr}/iccid/${iccid}")
                .request()
                .get()
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun testActivateEsim() {
        val iccid = "8901000000000000001"
        val eid = "01010101010101010101010101010101"
        val response = client.target("http://localhost:${SIM_MANAGER_RULE.getLocalPort()}/ostelco/sim-inventory/${hlr}/esim/${eid}")
                .queryParam("iccid", iccid)
                .request()
                .post(Entity.json(null))
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun testActivateNextEsim() {
        val eid = "10101010101010101010101010101010"
        val response = client.target("http://localhost:${SIM_MANAGER_RULE.getLocalPort()}/ostelco/sim-inventory/${hlr}/esim/${eid}")
                .request()
                .post(Entity.json(null))
        assertThat(response.status).isEqualTo(200)
    }
}
