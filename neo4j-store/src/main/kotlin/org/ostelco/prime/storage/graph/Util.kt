package org.ostelco.prime.storage.graph

import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

// Helper for naming of default segments based on country code.
fun getSegmentNameFromCountryCode(countryCode: String): String = "country-$countryCode".toLowerCase()

// Helper for naming of default plan segments based on country code.
fun getPlanSegmentNameFromCountryCode(countryCode: String): String = "plan-country-$countryCode".toLowerCase()

private val dfs = DecimalFormatSymbols().apply {
    groupingSeparator = '_'
}

private val df = DecimalFormat("#,###", dfs)

fun createProduct(sku: String, amount: Int, taxRegionId: String) =
        createProduct(sku, amount).copy(
            payment = mapOf(
                    "taxRegionId" to taxRegionId)
        )

fun createProduct(sku: String, amount: Int): Product {

    // This is messy code
    val gbs: Long = "${sku[0]}".toLong()

    return Product(
            sku = sku,
            price = Price(amount = amount, currency = "NOK"),
            payment = emptyMap(),
            properties = mapOf(
                    "noOfBytes" to df.format(gbs * Math.pow(2.0, 30.0).toLong()),
                    "productClass" to "SIMPLE_DATA"),
            presentation = mapOf("label" to "$gbs GB for ${amount / 100}"))
}