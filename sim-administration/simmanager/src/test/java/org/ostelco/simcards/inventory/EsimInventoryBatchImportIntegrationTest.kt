package org.ostelco.simcards.inventory

import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.jdbi.DBIFactory
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import junit.framework.TestCase
import org.junit.*
import org.ostelco.simcards.admin.SimAdministrationApplication
import org.skife.jdbi.v2.DBI
import java.math.BigInteger
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType


class EsimInventoryBatchImportTest {

    companion object {
        private lateinit var jdbi: DBI

        @JvmField
        @ClassRule
        val RULE = DropwizardAppRule(SimAdministrationApplication::class.java,
                ResourceHelpers.resourceFilePath("config.yml"))

        @BeforeClass
        @JvmStatic
        fun setUpDb() {
            jdbi = DBIFactory().build(RULE.environment,
                    RULE.configuration.database,
                    "db")
        }
    }

    val hlr = "Loltel"
    val profileVendor = "Idemia"

    @Before
    fun init() {
        clearTables()
        presetTables()
    }

    private fun clearTables() {
        val dao = jdbi.onDemand(SimInventoryCreationDestructionDAO::class.java)

        dao.clearTables()
    }

    private fun presetTables() {
        val dao = RULE.getApplication<SimAdministrationApplication>().DAO

        dao.addProfileVendorAdapter(profileVendor)
        dao.addHlrAdapter(hlr)
        dao.permitVendorForHlrByNames(profileVendor = profileVendor, hlr = hlr)
    }

    // FIXME make this test work in CircleCI
    @Test
    @Ignore("Fails on CircleCI")
    fun testImportIntegration() {

        val sampleValue = SimFactoryEmulator().simBatchOutFileAsString()

        val client = JerseyClientBuilder(RULE.environment)
                .using(RULE.configuration.getJerseyClientConfiguration())
                .build("Test client")


       // XXX This thing should be refactored into a client.
        val response = client
                .target("http://localhost:${RULE.getLocalPort()}/ostelco/sim-inventory/${hlr}/import-batch/profilevendor/${profileVendor}")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(sampleValue, MediaType.TEXT_PLAIN))

        // XXX Should be 201, but we'll accept a 200 for now.
        TestCase.assertEquals(200, response.status)

        val simEntry = response.readEntity(SimImportBatch::class.java)
    }
}

class SimFactoryEmulator {

    private val imsiStart :BigInteger  = BigInteger.valueOf(410072821393853L)
    private val iccidStart: BigInteger = BigInteger.valueOf(1234567890123456789L)
    private var rollingNumber = 0L

    private fun imsi(i: Int): String {
        return imsiStart.add(BigInteger.valueOf(i.toLong())).toString()
    }

    private fun iccid(i: Int): String {
        return iccidStart.add(BigInteger.valueOf(i.toLong())).toString()
    }

    private fun nextFourDigitNumber(): String {
        rollingNumber += 1
        return "%04d".format(rollingNumber % 10000)
    }

    fun simBatchOutFileAsString(): String {
        // NOTE: Doesn't scale up very far, should scale to several million before we're happy
        val header = "ICCID, IMSI, PIN1, PIN2, PUK1, PUK2\n"

        val sample = StringBuilder(header)
        for (i in 1..100) { // Works well up to 10000, after that it breaks
            val s = "%s, %s, %s, %s, %s, %s\n".format(
                    iccid(i),
                    imsi(i),
                    nextFourDigitNumber(),
                    nextFourDigitNumber(),
                    nextFourDigitNumber(),
                    nextFourDigitNumber())
            sample.append(s)
        }
        return sample.toString()
    }
}

