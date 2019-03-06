package org.ostelco.prime.notifications.email

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIf
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.module.getResource
import org.ostelco.prime.notifications.EmailNotifier

class TestApp : Application<TestConfig>() {

    override fun initialize(bootstrap: Bootstrap<TestConfig>) {
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
                bootstrap.configurationSourceProvider,
                EnvironmentVariableSubstitutor(false))
    }

    override fun run(configuration: TestConfig, environment: Environment) {
        configuration.modules.forEach { it.init(environment) }
    }
}

class TestConfig: Configuration() {

    @JsonProperty
    lateinit var modules: List<PrimeModule>
}

class MandrillClientTest {

    @DisabledIf("systemEnvironment.get('MANDRILL_API_KEY') == null")
    @Test
    fun `send test email via Mandrill`() {

        TestApp().run("server", "src/test/resources/config.yaml")

        Thread.sleep(3000)

        val emailNotifier = getResource<EmailNotifier>()

        emailNotifier.sendESimQrCodeEmail(email = "vihang@redotter.sg", name = "Vihang", qrCode = "Hello")
    }
}