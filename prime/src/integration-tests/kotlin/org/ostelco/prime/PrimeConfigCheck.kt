package org.ostelco.prime

import io.dropwizard.Application
import io.dropwizard.cli.CheckCommand
import io.dropwizard.cli.Command
import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.DropwizardTestSupport
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.util.*
import java.util.function.Function

class PrimeConfigCheck {

    /**
     * Do nothing.
     * This test will just start and stop the server.
     * It will validate config file in 'config/config.yaml'
     */
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
                    Function<Application<PrimeConfiguration>, Command> { app -> CheckCommand<PrimeConfiguration>(app) },
                    ConfigOverride.config("modules[3].config.configFile","config/prime-service-account.json"),
                    ConfigOverride.config("modules[11].config.configFile","config/prime-service-account.json")
            )

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            SUPPORT.before()
        }
    }
}