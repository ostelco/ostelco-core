import org.ostelco.prime.dsl.DSL.job
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass.SIMPLE_DATA
import org.ostelco.prime.model.ProductProperties.NO_OF_BYTES
import org.ostelco.prime.model.ProductProperties.PRODUCT_CLASS
import org.ostelco.prime.model.Region
import org.ostelco.prime.model.Segment
import org.ostelco.prime.storage.graph.Neo4jStoreSingleton.atomicCreateOffer
import org.ostelco.prime.storage.graph.Neo4jStoreSingleton.createIndex
import org.ostelco.prime.storage.graph.createProduct


private val logger by getLogger()

createIndex()

// Generic

job {
    create { Product(
            sku = "2GB_FREE_ON_JOINING",
            price = Price(0, ""),
            properties = mapOf(
                    PRODUCT_CLASS.s to SIMPLE_DATA.name,
                    NO_OF_BYTES.s to "2_147_483_648"
            ))
    }
    create {
        Product(
                sku = "1GB_FREE_ON_REFERRED",
                price = Price(0, ""),
                properties = mapOf(
                        PRODUCT_CLASS.s to SIMPLE_DATA.name,
                        NO_OF_BYTES.s to "1_073_741_824"
                ))
    }
}.mapLeft {
    throw Exception(it.message)
}

// For Norway

job {
    create { Region(id = "no", name = "Norway") }
}.mapLeft {
    throw Exception(it.message)
}

atomicCreateOffer(
        offer = Offer(id = "default_offer"),
        segments = listOf(Segment(id = "country-no")),
        products = listOf(
                createProduct(sku = "1GB_249NOK"),
                createProduct(sku = "2GB_299NOK"),
                createProduct(sku = "3GB_349NOK"),
                createProduct(sku = "5GB_399NOK"))
).mapLeft {
    throw Exception(it.message)
}