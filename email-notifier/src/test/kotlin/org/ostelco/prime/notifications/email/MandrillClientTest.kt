package org.ostelco.prime.notifications.email

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.module.getResource
import org.ostelco.prime.notifications.EmailNotifier

class TestApp : Application<TestConfig>() {

    override fun initialize(bootstrap: Bootstrap<TestConfig>) {
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
                bootstrap.configurationSourceProvider,
                EnvironmentVariableSubstitutor(false))
        bootstrap.objectMapper.registerModule(KotlinModule())
    }

    override fun run(configuration: TestConfig, environment: Environment) {
        configuration.modules.forEach { it.init(environment) }
    }
}

data class TestConfig(val modules: List<PrimeModule>): Configuration()

class MandrillClientTest {

    @EnabledIfEnvironmentVariable(named = "MANDRILL_API_KEY", matches = ".*")
    @Test
    fun `send test email via Mandrill`() {

        TestApp().run("server", "src/test/resources/config.yaml")

        Thread.sleep(3000)

        val emailNotifier = getResource<EmailNotifier>()

        emailNotifier.sendESimQrCodeEmail(email = "vihang@redotter.sg", name = "Vihang", qrCode = "Hello")
    }
}