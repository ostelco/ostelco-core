package org.ostelco.pseudonymiser

import com.google.gson.JsonParser
import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.glassfish.jersey.client.JerseyClientBuilder
import org.junit.ClassRule
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

/**
 * Class to do integration testing of  pseudonymiser.
 */
class PseudonymiserTest {

    private val msisdn = "4790303333"
    companion object {

        @JvmField
        @ClassRule
        val RULE = DropwizardAppRule(
                PseudonymiserApplication::class.java,
                ResourceHelpers.resourceFilePath("config.yaml"),
                ConfigOverride.config("datastoreType", "emulator"))
    }

    /**
     * Test a normal request
     */
    @Test
    fun testPseudonymiser() {

        val response = JerseyClientBuilder().build()
                ?.target("http://0.0.0.0:${RULE.getLocalPort()}/pseudonymiser/status")
                ?.request()
                ?.get()
        assertEquals(200, response?.status)

    }
}
