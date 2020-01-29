package org.ostelco.tools.prime.admin

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.getOrElse
import org.ostelco.prime.dsl.readTransaction
import org.ostelco.prime.dsl.withCode
import org.ostelco.prime.dsl.withSku
import org.ostelco.prime.model.PaymentProperties.LABEL
import org.ostelco.prime.model.PaymentProperties.TAX_REGION_ID
import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass.SIMPLE_DATA
import org.ostelco.prime.model.ProductProperties.NO_OF_BYTES
import org.ostelco.prime.model.ProductProperties.PRODUCT_CLASS
import org.ostelco.prime.model.Region
import org.ostelco.prime.storage.StoreError
import kotlin.test.assertEquals

println("Started Checking")

readTransaction {
    Either.fx<StoreError, Unit> {

        // for Norway
        assertEquals(
                expected = Region(id = "no", name = "Norway"),
                actual = get(Region withCode "no").bind()
        )

        assertEquals(
                expected = Product(sku = "1GB_0NOK",
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
                actual = get(Product withSku  "1GB_0NOK").bind()
        )

        assertEquals(
                expected = Product(sku = "1GB_249NOK",
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
                actual = get(Product withSku  "1GB_249NOK").bind()
        )

        assertEquals(
                expected = Product(sku = "2GB_299NOK",
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
                actual = get(Product withSku  "2GB_299NOK").bind()
        )

        assertEquals(
                expected = Product(sku = "3GB_349NOK",
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
                actual = get(Product withSku  "3GB_349NOK").bind()
        )

        assertEquals(
                expected = Product(sku = "5GB_399NOK",
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
                ),
                actual = get(Product withSku  "5GB_399NOK").bind()
        )

        // for Singapore

        assertEquals(
                expected = Region(id = "sg", name = "Singapore"),
                actual = get(Region withCode "sg").getOrElse { null })

        assertEquals(
                expected = Product(sku = "1GB_5SGD",
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
                actual = get(Product withSku  "1GB_5SGD").bind()
        )

        assertEquals(
                expected = Product(sku = "5GB_20SGD",
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
                ),
                actual = get(Product withSku  "5GB_20SGD").bind()
        )

        assertEquals(
                expected = Product(sku = "1GB_2SGD",
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
                actual = get(Product withSku  "1GB_2SGD").bind()
        )

        assertEquals(
                expected = Product(sku = "5GB_5SGD",
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
                ),
                actual = get(Product withSku  "5GB_5SGD").bind()
        )

        assertEquals(
                expected = Product(sku = "1GB_4SGD",
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
                actual = get(Product withSku  "1GB_4SGD").bind()
        )

        assertEquals(
                expected = Product(sku = "5GB_15SGD",
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
                ),
                actual = get(Product withSku  "5GB_15SGD").bind()
        )


        // for US

        assertEquals(
                expected = Region(id = "us", name = "United States"),
                actual = get(Region withCode "us").getOrElse { null }
        )

        assertEquals(
                expected = Product(sku = "1GB_5USD",
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
                ),
                actual = get(Product withSku  "1GB_5USD").bind()
        )


        // for MY

        assertEquals(
                expected = Region(id = "my", name = "Malaysia"),
                actual = get(Region withCode "my").getOrElse { null }
        )


        // generic

        assertEquals(
                expected = Product(sku = "2GB_FREE_ON_JOINING",
                        price = Price(0, ""),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "2_147_483_648"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "Free",
                                "productLabel" to "2GB Welcome Pack"
                        )
                ),
                actual = get(Product withSku  "2GB_FREE_ON_JOINING").bind()
        )

        assertEquals(
                expected = Product(sku = "1GB_FREE_ON_JOINING",
                        price = Price(0, ""),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "1_073_741_824"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "Free",
                                "productLabel" to "1GB Welcome Pack"
                        )
                ),
                actual = get(Product withSku  "1GB_FREE_ON_JOINING").bind()
        )

        assertEquals(
                expected = Product(sku = "1GB_FREE_ON_REFERRED",
                        price = Price(0, ""),
                        properties = mapOf(
                                PRODUCT_CLASS.s to SIMPLE_DATA.name,
                                NO_OF_BYTES.s to "1_073_741_824"
                        ),
                        presentation = mapOf(
                                "priceLabel" to "Free",
                                "productLabel" to "1GB Referral Pack"
                        )
                ),
                actual = get(Product withSku  "1GB_FREE_ON_REFERRED").bind()
        )
    }.mapLeft { println(it.message) }
}

println("Checking complete")