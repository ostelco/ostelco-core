import org.ostelco.prime.dsl.DSL.job
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.PaymentProperties.LABEL
import org.ostelco.prime.model.PaymentProperties.TAX_REGION_ID
import org.ostelco.prime.model.PaymentProperties.TYPE
import org.ostelco.prime.model.PaymentType.SUBSCRIPTION
import org.ostelco.prime.model.Plan
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass.MEMBERSHIP
import org.ostelco.prime.model.ProductClass.SIMPLE_DATA
import org.ostelco.prime.model.ProductProperties.NO_OF_BYTES
import org.ostelco.prime.model.ProductProperties.PRODUCT_CLASS
import org.ostelco.prime.model.ProductProperties.SEGMENT_IDS
import org.ostelco.prime.model.Region
import org.ostelco.prime.model.Segment
import org.ostelco.prime.storage.graph.Neo4jStoreSingleton.atomicCreateOffer
import org.ostelco.prime.storage.graph.Neo4jStoreSingleton.createIndex
import org.ostelco.prime.storage.graph.adminStore
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

// For Singapore

job {
    create { Region(id = "sg", name = "Singapore") }
}.mapLeft {
    throw Exception(it.message)
}

adminStore.createPlan(
        plan = Plan(
                id = "PLAN_1000SGD_YEAR",
                interval = "year"),
        stripeProductName = "Annual subscription plan",
        planProduct = Product(
                sku = "PLAN_1000SGD_YEAR",
                price = Price(amount = 1_000_00, currency = "SGD"),
                properties = mapOf(
                        PRODUCT_CLASS.s to MEMBERSHIP.name,
                        SEGMENT_IDS.s to "country-sg"),
                payment = mapOf(
                        TYPE.s to SUBSCRIPTION.name,
                        LABEL.s to "Annual subscription plan",
                        TAX_REGION_ID.s to "sg"
                )
        )
).mapLeft {
    throw Exception(it.message)
}

adminStore.atomicCreateOffer(
        offer = Offer(
                id = "plan-offer-sg",
                products = listOf("PLAN_1000SGD_YEAR")
        ),
        segments = listOf(Segment(id = "plan-country-sg"))
).mapLeft {
    throw Exception(it.message)
}

adminStore.atomicCreateOffer(
        offer = Offer(id = "default_offer-sg"),
        segments = listOf(Segment(id = "country-sg")),
        products = listOf(
                Product(sku = "1GB_100SGD",
                        price = Price(100_00, "SGD"),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "1_073_741_824"
                        ),
                        payment = mapOf(
                                LABEL.s to "1GB",
                                TAX_REGION_ID.s to "sg"
                        )
                )
        )
).mapLeft {
    throw Exception(it.message)
}