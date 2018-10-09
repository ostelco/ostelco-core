package org.ostelco.prime.imei.imeilookup

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.junit.Before
import org.junit.Test
import org.ostelco.prime.getLogger
import org.ostelco.prime.imei.ImeiLookup
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.module.getResource
import kotlin.test.assertEquals


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


class ImeiSqliteDbTest {

    private val imeiLookup by lazy { getResource<ImeiLookup>() }

    companion object {
        init {
            TestApp().run("server", "src/test/resources/config.yaml")
        }
    }

    @Test
    fun getImeiResult() {
        val result = imeiLookup.getImeiInformation("3550900831237501")
        assertEquals(true, result.isRight())
    }

}
