package org.ostelco.tools.prime.admin

import arrow.core.flatMap
import org.ostelco.prime.dsl.withId
import org.ostelco.prime.dsl.writeTransaction
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.PaymentProperties.LABEL
import org.ostelco.prime.model.PaymentProperties.TAX_REGION_ID
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass.SIMPLE_DATA
import org.ostelco.prime.model.ProductProperties.NO_OF_BYTES
import org.ostelco.prime.model.ProductProperties.PRODUCT_CLASS
import org.ostelco.prime.storage.graph.adminStore
import org.ostelco.prime.storage.graph.model.Offer
import org.ostelco.prime.storage.graph.model.Segment
import org.ostelco.prime.model.Offer as ModelOffer

private val logger by getLogger()

/**
 * Done on 2019.11.27
 */

adminStore.atomicCreateOffer(
        offer = ModelOffer(
                id = "discount-sgd",
                segments = listOf("country-sg")
        ),
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
).flatMap {
    writeTransaction {
        unlink { (Offer withId "default_offer-sg") isOfferedTo (Segment withId "country-sg") }
    }
}.mapLeft {
    logger.error(it.message)
}

