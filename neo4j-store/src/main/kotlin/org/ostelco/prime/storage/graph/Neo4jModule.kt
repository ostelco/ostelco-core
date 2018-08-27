package org.ostelco.prime.storage.graph

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.lifecycle.Managed
import io.dropwizard.setup.Environment
import org.neo4j.driver.v1.AuthTokens
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.GraphDatabase
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.Segment
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
        if (System.getenv("FIREBASE_ROOT_PATH") == "test") {
            initDatabase()
        }
    }
}

fun initDatabase() {
    Neo4jStoreSingleton.createProduct(createProduct(sku = "1GB_249NOK", amount = 24900))
    Neo4jStoreSingleton.createProduct(createProduct(sku = "2GB_299NOK", amount = 29900))
    Neo4jStoreSingleton.createProduct(createProduct(sku = "3GB_349NOK", amount = 34900))
    Neo4jStoreSingleton.createProduct(createProduct(sku = "5GB_399NOK", amount = 39900))

    Neo4jStoreSingleton.createProduct(Product(
            sku = "100MB_FREE_ON_JOINING",
            price = Price(0, "NOK"),
            properties = mapOf("noOfBytes" to "100_000_000")))
    Neo4jStoreSingleton.createProduct(Product(
            sku = "1GB_FREE_ON_REFERRED",
            price = Price(0, "NOK"),
            properties = mapOf("noOfBytes" to "1_000_000_000")))

    val segment = Segment(id = "all")
    Neo4jStoreSingleton.createSegment(segment)

    val offer = Offer(
            id = "default_offer",
            segments = listOf("all"),
            products = listOf("1GB_249NOK", "2GB_299NOK", "3GB_349NOK", "5GB_399NOK"))
    Neo4jStoreSingleton.createOffer(offer)
}

class Config {
    lateinit var host: String
    lateinit var protocol: String
}

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

fun createProduct(sku: String, amount: Int): Product {

    // This is messy code
    val gbs: Long = "${sku[0]}".toLong()

    return Product(
            sku = sku,
            price = Price(amount = amount, currency = "NOK"),
            properties = mapOf("noOfBytes" to "${gbs}_000_000_000"),
            presentation = mapOf("label" to "$gbs GB for ${amount / 100}"))
}
