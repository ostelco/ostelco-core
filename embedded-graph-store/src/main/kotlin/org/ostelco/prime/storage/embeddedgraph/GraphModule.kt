package org.ostelco.prime.storage.embeddedgraph

import com.codahale.metrics.health.HealthCheck
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.lifecycle.Managed
import io.dropwizard.setup.Environment
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.kernel.configuration.BoltConnector
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.module.PrimeModule
import java.io.File

@JsonTypeName("embedded-graph")
class GraphModule : PrimeModule {

    override fun init(env: Environment) {
        env.lifecycle().manage(GraphServer)
        env.healthChecks().register("Embedded graph server", GraphServer)

        // starting explicitly since OCS needs it during its init() to load balance
        GraphServer.start()

        // FIXME remove adding of dummy data in Graph DB
        GraphStoreSingleton.createProduct(createProduct("1GB_249NOK", 24900))
        GraphStoreSingleton.createProduct(createProduct("2GB_299NOK", 29900))
        GraphStoreSingleton.createProduct(createProduct("3GB_349NOK", 34900))
        GraphStoreSingleton.createProduct(createProduct("5GB_399NOK", 39900))
        GraphStoreSingleton.addSubscription("foo@bar.com","4747900184")
        GraphStoreSingleton.setBalance("4747900184",1_000_000_000L)
        GraphStoreSingleton.addSubscriber(Subscriber(email = "foo@bar.com", name = "Test User"))
    }
}

object GraphServer : Managed, HealthCheck() {

    var bolt: BoltConnector = BoltConnector("0")

    lateinit var graphDb: GraphDatabaseService
        private set

    private var isRunning: Boolean = false;

    override fun start() {

        if (isRunning) {
            return
        }

        graphDb = GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(File("build/neo4j"))
                .setConfig(bolt.enabled, "true")
                .setConfig(bolt.type, "BOLT")
                .setConfig(bolt.listen_address, "localhost:7687")
                .newGraphDatabase()

        isRunning = true
    }

    override fun stop() {
        graphDb.shutdown()
        isRunning = false
    }

    override fun check(): Result {
        if (isRunning) {
            return Result.healthy()
        } else {
            return Result.unhealthy("Embedded graph server not running")
        }
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
    product.properties = mapOf("noOfBytes" to "${gbs*1024*1024*1024}")
    product.presentation = mapOf("label" to "$gbs GB for ${amount/100}")

    return product
}