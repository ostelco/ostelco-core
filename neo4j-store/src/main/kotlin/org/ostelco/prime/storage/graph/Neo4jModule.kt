package org.ostelco.prime.storage.graph

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.lifecycle.Managed
import io.dropwizard.setup.Environment
import org.neo4j.driver.v1.AuthTokens
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.GraphDatabase
import org.ostelco.prime.kts.engine.KtsServiceFactory
import org.ostelco.prime.kts.engine.reader.ClasspathResourceTextReader
import org.ostelco.prime.kts.engine.script.RunnableKotlinScript
import org.ostelco.prime.module.PrimeModule
import java.net.URI
import java.util.concurrent.TimeUnit.SECONDS

@JsonTypeName("neo4j")
class Neo4jModule : PrimeModule {

    @JsonProperty
    fun setConfig(config: Config) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
        env.lifecycle().manage(Neo4jClient)

        // starting explicitly since OCS needs it during its init() to load balance
        Neo4jClient.start()

        // For Acceptance Tests
        if (System.getenv("ACCEPTANCE_TESTING") == "true") {
            RunnableKotlinScript(ClasspathResourceTextReader("/AcceptanceTestSetup.kts").readText()).eval<Any?>()
        }
    }
}

data class Config(
        val host: String,
        val protocol: String,
        val hssNameLookupService: KtsServiceFactory,
        val onNewCustomerAction: KtsServiceFactory)

object ConfigRegistry {
    lateinit var config: Config
}

object Neo4jClient : Managed {

    // use "bolt+routing://neo4j:7687" for clustered Neo4j
    // Explore config and auth
    lateinit var driver: Driver

    override fun start() {
        val config = org.neo4j.driver.v1.Config.build()
                .withoutEncryption()
                .withConnectionTimeout(10, SECONDS)
                .withMaxConnectionPoolSize(1000)
                .toConfig()
        driver = GraphDatabase.driver(
                URI("${ConfigRegistry.config.protocol}://${ConfigRegistry.config.host}:7687"),
                AuthTokens.none(),
                config) ?: throw Exception("Unable to get Neo4j client driver instance")
    }

    override fun stop() {
        driver.close()
    }
}
