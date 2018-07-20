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
import org.ostelco.prime.model.Subscriber
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

private fun initDatabase() {
    Neo4jStoreSingleton.createProduct(createProduct("1GB_249NOK", 24900))
    Neo4jStoreSingleton.createProduct(createProduct("2GB_299NOK", 29900))
    Neo4jStoreSingleton.createProduct(createProduct("3GB_349NOK", 34900))
    Neo4jStoreSingleton.createProduct(createProduct("5GB_399NOK", 39900))

    Neo4jStoreSingleton.addSubscriber(Subscriber(email = "foo@bar.com", name = "Test User"))
    Neo4jStoreSingleton.addSubscription("foo@bar.com", "4747900184")
    Neo4jStoreSingleton.setBalance("4747900184", 1_000_000_000L)

    val segment = Segment(listOf("foo@bar.com"))
    segment.id = "all"
    Neo4jStoreSingleton.createSegment(segment)

    val offer = Offer(listOf("all"), listOf("1GB_249NOK", "2GB_299NOK", "3GB_349NOK", "5GB_399NOK"))
    offer.id = "default_offer"
    Neo4jStoreSingleton.createOffer(offer)
}

class Config {
    lateinit var host: String
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
                URI("bolt://${ConfigRegistry.config.host}:7687"),
                AuthTokens.none(),
                config) ?: throw Exception("Unable to get Neo4j client driver instance")
    }

    override fun stop() {
        driver.close()
    }
}

fun createProduct(sku: String, amount: Int): Product {
    val product = Product()
    product.sku = sku
    product.price = Price()
    product.price.amount = amount
    product.price.currency = "NOK"

    // This is messy code
    val gbs: Long = "${sku[0]}".toLong()
    product.properties = mapOf("noOfBytes" to "${gbs * 1024 * 1024 * 1024}")
    product.presentation = mapOf("label" to "$gbs GB for ${amount / 100}")

    return product
}
