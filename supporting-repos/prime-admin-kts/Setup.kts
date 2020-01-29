package org.ostelco.tools.prime.admin

import org.ostelco.prime.dsl.DSL.job
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.PaymentProperties.LABEL
import org.ostelco.prime.model.PaymentProperties.TAX_REGION_ID
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass.SIMPLE_DATA
import org.ostelco.prime.model.ProductProperties.NO_OF_BYTES
import org.ostelco.prime.model.ProductProperties.PRODUCT_CLASS
import org.ostelco.prime.model.Region
import org.ostelco.prime.model.Segment
import org.ostelco.prime.storage.graph.adminStore

private val logger by getLogger()

println("Started Setup")

// for Norway

job {
    create { Region(id = "no", name = "Norway") }
}.mapLeft {
    logger.error(it.message)
}

adminStore.atomicCreateOffer(
        offer = Offer(id = "default_offer-no"),
        segments = listOf(Segment(id = "country-no")),
        products = listOf(
                Product(sku = "1GB_0NOK",
                        price = Price(0, ""),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "1_073_741_824"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "Free",
                                "productLabel" to "1GB",
                                "payeeLabel" to "Red Otter",
                                "subTotal" to "0",
                                "taxLabel" to "MVA",
                                "tax" to "0",
                                "subTotalLabel" to "Sub Total"
                        ),
                        payment = mapOf(
                                LABEL.s to "1GB",
                                TAX_REGION_ID.s to "no"
                        )
                ),
                Product(sku = "1GB_249NOK",
                        price = Price(24900, "NOK"),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "1_073_741_824"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "249 kr",
                                "productLabel" to "1GB",
                                "payeeLabel" to "Red Otter",
                                "subTotal" to "19920",
                                "taxLabel" to "MVA",
                                "tax" to "4980",
                                "subTotalLabel" to "Sub Total"
                        ),
                        payment = mapOf(
                                LABEL.s to "1GB",
                                TAX_REGION_ID.s to "no"
                        )
                ),
                Product(sku = "2GB_299NOK",
                        price = Price(29900, "NOK"),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "2_147_483_648"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "299 kr",
                                "productLabel" to "2GB",
                                "payeeLabel" to "Red Otter",
                                "subTotal" to "23920",
                                "taxLabel" to "MVA",
                                "tax" to "5980",
                                "subTotalLabel" to "Sub Total"
                        ),
                        payment = mapOf(
                                LABEL.s to "2GB",
                                TAX_REGION_ID.s to "no"
                        )
                ),
                Product(sku = "3GB_349NOK",
                        price = Price(34900, "NOK"),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "3_221_225_472"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "349 kr",
                                "productLabel" to "3GB",
                                "payeeLabel" to "Red Otter",
                                "subTotal" to "27920",
                                "taxLabel" to "MVA",
                                "tax" to "6980",
                                "subTotalLabel" to "Sub Total"
                        ),
                        payment = mapOf(
                                LABEL.s to "3GB",
                                TAX_REGION_ID.s to "no"
                        )
                ),
                Product(sku = "5GB_399NOK",
                        price = Price(39900, "NOK"),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "5_368_709_120"),
                        presentation = mapOf(
                                "priceLabel" to "399 kr",
                                "productLabel" to "5GB",
                                "payeeLabel" to "Red Otter",
                                "subTotal" to "31920",
                                "taxLabel" to "MVA",
                                "tax" to "7980",
                                "subTotalLabel" to "Sub Total"
                        ),
                        payment = mapOf(
                                LABEL.s to "5GB",
                                TAX_REGION_ID.s to "no"
                        )
                )
        )
).mapLeft {
    logger.error(it.message)
}

// for Singapore
job {
    create { Region(id = "sg", name = "Singapore") }
}

adminStore.atomicCreateOffer(
        offer = Offer(id = "default_offer-sg"),
        segments = listOf(Segment(id = "country-sg")),
        products = listOf(
                Product(sku = "1GB_5SGD",
                        price = Price(500, "SGD"),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "1_073_741_824"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "S$5",
                                "productLabel" to "1GB",
                                "payeeLabel" to "Red Otter",
                                "subTotal" to "467",
                                "taxLabel" to "GST",
                                "tax" to "33",
                                "subTotalLabel" to "Sub Total"
                        ),
                        payment = mapOf(
                                LABEL.s to "1GB",
                                TAX_REGION_ID.s to "sg"
                        )
                ),
                Product(sku = "5GB_20SGD",
                        price = Price(2000, "SGD"),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "5_368_709_120"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "S$20",
                                "productLabel" to "5GB",
                                "payeeLabel" to "Red Otter",
                                "subTotal" to "1868",
                                "taxLabel" to "GST",
                                "tax" to "132",
                                "subTotalLabel" to "Sub Total"
                        ),
                        payment = mapOf(
                                LABEL.s to "5GB",
                                TAX_REGION_ID.s to "sg"
                        )
                )
        )
).mapLeft {
    logger.error(it.message)
}

adminStore.atomicCreateOffer(
        offer = Offer(id = "discount-sgd"),
        segments = listOf(Segment(id = "country-sg")),
        products = listOf(
                Product(sku = "1GB_2SGD",
                        price = Price(200, "SGD"),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "1_073_741_824"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "S$2",
                                "productLabel" to "1GB",
                                "payeeLabel" to "Red Otter",
                                "subTotal" to "187",
                                "taxLabel" to "GST",
                                "tax" to "13",
                                "subTotalLabel" to "Sub Total"
                        ),
                        payment = mapOf(
                                LABEL.s to "1GB",
                                TAX_REGION_ID.s to "sg"
                        )
                ),
                Product(sku = "5GB_5SGD",
                        price = Price(500, "SGD"),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "5_368_709_120"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "S$5",
                                "productLabel" to "5GB",
                                "payeeLabel" to "Red Otter",
                                "subTotal" to "467",
                                "taxLabel" to "GST",
                                "tax" to "33",
                                "subTotalLabel" to "Sub Total"
                        ),
                        payment = mapOf(
                                LABEL.s to "5GB",
                                TAX_REGION_ID.s to "sg"
                        )
                )
        )
).mapLeft {
    logger.error(it.message)
}

adminStore.atomicCreateOffer(
        offer = Offer(id = "currency-sgd"),
        segments = listOf(Segment(id = "country-sg")),
        products = listOf(
                Product(sku = "1GB_4SGD",
                        price = Price(400, "SGD"),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "1_073_741_824"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "S$4",
                                "productLabel" to "1GB",
                                "payeeLabel" to "Red Otter",
                                "subTotal" to "374",
                                "taxLabel" to "GST",
                                "tax" to "26",
                                "subTotalLabel" to "Sub Total"
                        ),
                        payment = mapOf(
                                LABEL.s to "1GB",
                                TAX_REGION_ID.s to "sg"
                        )
                ),
                Product(sku = "5GB_15SGD",
                        price = Price(1500, "SGD"),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "5_368_709_120"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "S$15",
                                "productLabel" to "5GB",
                                "payeeLabel" to "Red Otter",
                                "subTotal" to "1402",
                                "taxLabel" to "GST",
                                "tax" to "98",
                                "subTotalLabel" to "Sub Total"
                        ),
                        payment = mapOf(
                                LABEL.s to "5GB",
                                TAX_REGION_ID.s to "sg"
                        )
                )
        )
).mapLeft {
    logger.error(it.message)
}

// for Malaysia
job {
    create { Region(id = "my", name = "Malaysia") }
}

// for US
job {
    create { Region(id = "us", name = "United States") }
}.mapLeft {
    logger.error(it.message)
}

adminStore.atomicCreateOffer(
        offer = Offer(id = "default_offer-us"),
        segments = listOf(Segment(id = "country-us")),
        products = listOf(
                Product(sku = "1GB_5USD",
                        price = Price(500, "USD"),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "1_073_741_824"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "$5",
                                "productLabel" to "1GB",
                                "payeeLabel" to "Red Otter",
                                "subTotal" to "401",
                                "taxLabel" to "GST",
                                "tax" to "99",
                                "subTotalLabel" to "Sub Total"
                        ),
                        payment = mapOf(
                                LABEL.s to "1GB",
                                TAX_REGION_ID.s to "us"
                        )
                )
        )
).mapLeft {
    logger.error(it.message)
}

// generic

job {
    create {
        Product(sku = "1GB_FREE_ON_JOINING",
                price = Price(0, ""),
                properties = mapOf(
                        PRODUCT_CLASS.s to SIMPLE_DATA.name,
                        NO_OF_BYTES.s to "1_073_741_824"
                ),
                presentation = mapOf(
                        "priceLabel" to "Free",
                        "productLabel" to "1GB Welcome Pack"
                )
        )
    }

    create {
        Product(sku = "1GB_FREE_ON_REFERRED",
                price = Price(0, ""),
                properties = mapOf(
                        PRODUCT_CLASS.s to SIMPLE_DATA.name,
                        NO_OF_BYTES.s to "1_073_741_824"
                ),
                presentation = mapOf(
                        "priceLabel" to "Free",
                        "productLabel" to "1GB Referral Pack"
                )
        )
    }
}.mapLeft {
    logger.error(it.message)
}

println("Setup Complete")