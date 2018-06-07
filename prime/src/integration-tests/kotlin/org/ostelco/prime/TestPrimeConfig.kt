package org.ostelco.prime

import io.dropwizard.testing.DropwizardTestSupport
import io.dropwizard.testing.ResourceHelpers
import org.junit.AfterClass
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Test

class TestPrimeConfig {

    /**
     * Do nothing.
     * This test will just start and stop the server.
     * It will validate config file in 'src/integration-tests/resources/config.yaml'
     */
    @Test
    fun test() {
        assertNotNull(SUPPORT)
    }

    companion object {

        private val SUPPORT = DropwizardTestSupport(PrimeApplication::class.java,
                ResourceHelpers.resourceFilePath("config.yaml"))

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            SUPPORT.before()
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            SUPPORT.after()
        }
    }
}
