
import org.ostelco.prime.dsl.DSL.job
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.Region
import org.ostelco.prime.model.Segment
import org.ostelco.prime.storage.graph.Neo4jStoreSingleton.atomicCreateOffer
import org.ostelco.prime.storage.graph.Neo4jStoreSingleton.createIndex
import org.ostelco.prime.storage.graph.createProduct
import org.ostelco.prime.storage.graph.getPlanSegmentNameFromCountryCode
import org.ostelco.prime.storage.graph.getSegmentNameFromCountryCode


createIndex()

job {
    create { Region(id = "no", name = "Norway") }
    create { Region(id = "sg", name = "Singapore") }
    create { Product(
            sku = "2GB_FREE_ON_JOINING",
            price = Price(0, ""),
            properties = mapOf(
                    "noOfBytes" to "2_147_483_648",
                    "productClass" to "SIMPLE_DATA"))
    }
    create {
        Product(
                sku = "1GB_FREE_ON_REFERRED",
                price = Price(0, ""),
                properties = mapOf(
                        "noOfBytes" to "1_073_741_824",
                        "productClass" to "SIMPLE_DATA"))
    }
    /* Note: (kmm) For 'sg' the first segment offered is always a plan. */
    create {
        Segment(id = getPlanSegmentNameFromCountryCode("SG"))
    }
}

atomicCreateOffer(
        offer = Offer(id = "default_offer"),
        segments = listOf(Segment(id = getSegmentNameFromCountryCode("NO"))),
        products = listOf(
                createProduct(sku = "1GB_249NOK", amount = 24900),
                createProduct(sku = "2GB_299NOK", amount = 29900),
                createProduct(sku = "3GB_349NOK", amount = 34900),
                createProduct(sku = "5GB_399NOK", amount = 39900))
)