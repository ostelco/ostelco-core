package org.ostelco.prime.storage.graph

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.lifecycle.Managed
import io.dropwizard.setup.Environment
import org.neo4j.driver.v1.GraphDatabase
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.module.PrimeModule
import java.net.URI

@JsonTypeName("neo4j")
class Neo4jModule : PrimeModule {

    override fun init(env: Environment) {
        env.lifecycle().manage(Neo4jClient)
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