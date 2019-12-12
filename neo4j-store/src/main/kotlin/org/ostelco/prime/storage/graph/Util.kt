package org.ostelco.prime.storage.graph

import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass.SIMPLE_DATA
import org.ostelco.prime.model.ProductProperties.NO_OF_BYTES
import org.ostelco.prime.model.ProductProperties.PRODUCT_CLASS
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AdminDataSource
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

val adminStore by lazy { getResource<AdminDataSource>() }

private val dfs = DecimalFormatSymbols().apply {
    groupingSeparator = '_'
}

private val df = DecimalFormat("#,###", dfs)

fun createProduct(sku: String, taxRegionId: String? = null): Product {

    val (bytes, price) = sku.split("_")

    val amount = (price.substring(0, price.length - 3).toFloat() * 100).toInt()
    val currency = price.substring(price.length - 3)

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

    return Product(
            sku = sku,
            price = Price(amount = amount, currency = currency),
            properties = mapOf(
                    PRODUCT_CLASS.s to SIMPLE_DATA.name,
                    NO_OF_BYTES.s to df.format(number * Math.pow(2.0, pow).toLong())
            ),
            presentation = mapOf("label" to "${number.toInt()} $unit for ${amount / 100}"),
            payment = if (taxRegionId != null) {
                mapOf("taxRegionId" to taxRegionId)
            } else {
                emptyMap()
            }
    )
}

/**
 *  Formatting of amounts.
 *  TODO (kmm) Update to use the java.text.NumberFormat API or the new
 *             JSR-354 Currency and Money API.
 */
fun formatMoney(amount: Int, currency: String): String = DecimalFormat("#,###.##")
        .format(amount / 100.0) + " ${currency.toUpperCase()}"

fun formatMoney(price: Price): String = formatMoney(price.amount, price.currency)
