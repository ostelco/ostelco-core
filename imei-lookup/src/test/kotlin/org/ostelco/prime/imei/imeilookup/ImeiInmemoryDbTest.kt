package org.ostelco.prime.imei.imeilookup

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.junit.Test
import org.ostelco.prime.imei.ImeiLookup
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.module.getResource
import kotlin.test.assertEquals


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

class ImeiInmemoryDbTest {

    private val imeiLookup by lazy { getResource<ImeiLookup>() }

    companion object {
        init {
            TestApp().run("server", "src/test/resources/config.yaml")
        }
    }

    @Test
    fun getImeiResult() {
        val result = imeiLookup.getImeiInformation("001007323123750")
        assertEquals(true, result.isRight())
    }

    fun getImeiSvResult() {
        val result = imeiLookup.getImeiInformation("0010073231237501")
        assertEquals(true, result.isRight())
    }

    @Test
    fun getImeiShortFailure() {
        val result = imeiLookup.getImeiInformation("001007323")
        assertEquals(true, result.isLeft())
    }

    @Test
    fun getImeiLargeFailure() {
        val result = imeiLookup.getImeiInformation("00100732312375012")
        assertEquals(true, result.isLeft())
    }
}
