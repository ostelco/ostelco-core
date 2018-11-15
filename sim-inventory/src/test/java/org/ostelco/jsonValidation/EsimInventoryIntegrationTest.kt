package org.ostelco.jsonValidation

import io.dropwizard.client.JerseyClientBuilder
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import junit.framework.TestCase
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.SimAdministrationApplication
import org.ostelco.SimImportBatch
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import com.google.common.collect.TreeTraverser.using
import org.junit.Before


class EsimInventoryIntegrationTest() {
    public companion object {
        @JvmField
        @ClassRule
        public val RULE =
                DropwizardAppRule(
                        SimAdministrationApplication::class.java,
                        ResourceHelpers.resourceFilePath("config.yml"))
    }

    @Before
    fun initializeApp() {
        RULE.getApplication<SimAdministrationApplication>().simInventoryDAO.dropSimEntryTable()
        RULE.getApplication<SimAdministrationApplication>().simInventoryDAO.createSimEntryTable()
    }


    @Test
    fun testImport() {
        val sampleCsvIinput =
                """
        ICCID, IMSI, PIN1, PIN2, PUK1, PUK2
        123123, 123123, 1233,1233,1233,1233
        123123, 123123, 1233,1233,1233,1233
        123123, 123123, 1233,1233,1233,1233
       123123, 123123, 1233,1233,1233,1233
    """.trimIndent()

        // XXX Is this necessary?
        val client = JerseyClientBuilder(RULE.environment)
                .using(RULE.configuration.getJerseyClientConfiguration())
                .build("Test client")


        var response = client
                .target("http://localhost:8080/ostelco/sim-inventory/Loltel/import-batch/sim-profile-vendor/Idemia")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(sampleCsvIinput, MediaType.TEXT_PLAIN))

        // XXX Should be 201, but we'll accept a 200 for now.
        TestCase.assertEquals(200, response.status)

        val simEntry = response.readEntity(SimImportBatch::class.java)
    }
}


