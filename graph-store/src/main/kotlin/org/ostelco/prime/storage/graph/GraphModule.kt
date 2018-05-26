package org.ostelco.prime.storage.graph

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.lifecycle.Managed
import io.dropwizard.setup.Environment
import org.neo4j.driver.v1.GraphDatabase
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.module.PrimeModule
import java.net.URI

@JsonTypeName("graph")
class GraphModule : PrimeModule {

    override fun init(env: Environment) {
        env.lifecycle().manage(Neo4jClient)

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

object Neo4jClient : Managed {

    // use "bolt+routing://neo4j:7687" for clustered Neo4j
    // Explore config and auth
    val driver = GraphDatabase.driver(URI("bolt://neo4j:7687"))

    override fun start() {}

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
    product.properties = mapOf("noOfBytes" to "${gbs*1024*1024*1024}")
    product.presentation = mapOf("label" to "$gbs GB for ${amount/100}")

    return product
}