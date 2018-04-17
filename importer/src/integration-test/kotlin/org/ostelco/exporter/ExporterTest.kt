package org.ostelco.importer

import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.glassfish.jersey.client.JerseyClientBuilder
import org.junit.ClassRule
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Class to do integration testing of  pseudonymiser.
 */
class PseudonymServerTest {

    private val msisdn = "4790303333"
    companion object {

        @JvmField
        @ClassRule
        val RULE = DropwizardAppRule(
                ImporterApplication::class.java,
                ResourceHelpers.resourceFilePath("config.yaml"))
    }

    /**
     * Test a normal request
     */
    @Test
    fun testImporter() {

        val response = JerseyClientBuilder().build()
                ?.target("http://0.0.0.0:${RULE.getLocalPort()}/importer/status")
                ?.request()
                ?.get()
        assertEquals(200, response?.status)

    }
}
