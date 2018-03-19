package org.ostelco.prime

import io.dropwizard.testing.DropwizardTestSupport
import io.dropwizard.testing.ResourceHelpers
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.ostelco.prime.config.PrimeConfiguration

import org.junit.Assert.assertNotNull

class TestPrimeConfig {

    /**
     * Do nothing.
     * This test will just start and stop the server.
     * It will validate config file in 'src/test/resources/config.yaml'
     */
    @Test
    fun test() {
        assertNotNull(SUPPORT)
    }

    companion object {

        private val SUPPORT = DropwizardTestSupport(PrimeApplication::class.java,
                ResourceHelpers.resourceFilePath("config.yaml"))

        @BeforeClass
        fun beforeClass() {
            SUPPORT.before()
        }

        @AfterClass
        fun afterClass() {
            SUPPORT.after()
        }
    }
}
