package org.ostelco.at.common

import org.ostelco.prime.customer.model.Price
import org.ostelco.prime.customer.model.Product
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

fun expectedProducts(): List<Product> {
    return listOf(
            createProduct("1GB_249NOK"),
            createProduct("2GB_299NOK"),
            createProduct("3GB_349NOK"),
            createProduct("5GB_399NOK")
    )
}

val expectedPlanProductSG: Product = Product()
        .sku("PLAN_1000SGD_YEAR")
        .price(Price().amount(1_000_00).currency("SGD"))
        .properties(
                mapOf(
                        "productClass" to "MEMBERSHIP",
                        "segmentIds" to "country-sg"
                )
        )
        .payment(
                mapOf(
                        "type" to "SUBSCRIPTION",
                        "label" to "Annual subscription plan",
                        "taxRegionId" to "sg"
                )
        )
        .presentation(emptyMap<String, String>())

val expectedPlanProductUS: Product = Product()
        .sku("PLAN_10USD_DAY")
        .price(Price().amount(10_00).currency("USD"))
        .properties(
                mapOf(
                        "productClass" to "MEMBERSHIP",
                        "segmentIds" to "country-us"
                )
        )
        .payment(
                mapOf(
                        "type" to "SUBSCRIPTION",
                        "label" to "Daily subscription plan",
                        "taxRegionId" to "us"
                )
        )
        .presentation(emptyMap<String, String>())

private val dfs = DecimalFormatSymbols().apply {
    groupingSeparator = '_'
}
private val df = DecimalFormat("#,###", dfs)

private fun createProduct(sku: String, taxRegionId: String? = null): Product {

    val (bytes, price) = sku.split("_")

    val product = Product()
    product.sku = sku
    product.price = Price()
    product.price.amount = (price.substring(0, price.length - 3).toFloat() * 100).toInt()
    product.price.currency = price.substring(price.length - 3)

    val number = bytes.substring(0, bytes.length - 2).toFloat()
    val unit = bytes.substring(bytes.length - 2)
    val pow = when (unit) {
        "KB" -> 10.0
        "MB" -> 20.0
        "GB" -> 30.0
        "TB" -> 40.0
        "PB" -> 50.0
        "XB" -> 60.0
        else -> 0.0
    }
    product.payment = if (taxRegionId != null) {
        mapOf("taxRegionId" to taxRegionId)
    } else {
        emptyMap()
    }
    product.properties = mapOf(
            "productClass" to "SIMPLE_DATA",
            "noOfBytes" to df.format(number * Math.pow(2.0, pow))
    )
    product.presentation = mapOf("label" to "${number.toInt()} $unit for ${product.price.amount / 100}")

    return product
}