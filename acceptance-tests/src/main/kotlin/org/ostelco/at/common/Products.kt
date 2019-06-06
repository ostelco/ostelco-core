package org.ostelco.at.common

import org.ostelco.prime.customer.model.Price
import org.ostelco.prime.customer.model.Product
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

fun expectedProducts(): List<Product> {
    return listOf(
            createProduct("1GB_249NOK", 24900),
            createProduct("2GB_299NOK", 29900),
            createProduct("3GB_349NOK", 34900),
            createProduct("5GB_399NOK", 39900))
}

fun expectedPlanProducts(): List<Product> {
    return listOf(
            createProduct("PLAN_1000SGD_YEAR", 100000))
}

private val dfs = DecimalFormatSymbols().apply {
    groupingSeparator = '_'
}
private val df = DecimalFormat("#,###", dfs)

private fun createProduct(sku: String, amount: Int, taxRegionId: String? = null): Product {
    val product = Product()
    product.sku = sku
    product.price = Price()
    product.price.amount = amount
    product.price.currency = "NOK"

    // This is messy code
    val gbs: Long = "${sku[0]}".toLong()
    product.payment = if (taxRegionId != null)
        mapOf(
                "taxRegionId" to taxRegionId
        )
    else
        emptyMap<String, String>()
    product.properties = mapOf(
            "noOfBytes" to df.format(gbs * Math.pow(2.0, 30.0)),
            "productClass" to "SIMPLE_DATA")
    product.presentation = mapOf("label" to "$gbs GB for ${amount / 100}")

    return product
}