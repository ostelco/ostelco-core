package org.ostelco.prime

import io.dropwizard.Application
import io.dropwizard.cli.CheckCommand
import io.dropwizard.cli.Command
import io.dropwizard.testing.DropwizardTestSupport
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import java.util.*
import java.util.function.Function

class PrimeConfigCheck {

    /**
     * Do nothing.
     * This test will just start and stop the server.
     * It will validate config file in 'config/config.yaml'
     */
    @Ignore  //TODO: Ignoring, since it fails, since it doesn't substitute for environment variables, and that leads to syntax errors in config for es2+
    @Test
    fun test() {
        Assert.assertNotNull(SUPPORT)
    }

    companion object {

        private val SUPPORT: DropwizardTestSupport<PrimeConfiguration>
            get() = DropwizardTestSupport(
                    PrimeApplication::class.java,
                    "config/config.yaml",
                    Optional.empty<String>(),
                    Function<Application<PrimeConfiguration>, Command> { app -> CheckCommand<PrimeConfiguration>(app) }
            )

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            SUPPORT.before()
        }
    }
}