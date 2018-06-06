package org.ostelco.at.common

import org.ostelco.prime.client.model.Price
import org.ostelco.prime.client.model.Product

fun expectedProducts(): List<Product> {
    return listOf(
            createProduct("1GB_249NOK", 24900),
            createProduct("2GB_299NOK", 29900),
            createProduct("3GB_349NOK", 34900),
            createProduct("5GB_399NOK", 39900))
}

private fun createProduct(sku: String, amount: Int): Product {
    val product = Product()
    product.sku = sku
    product.price = Price()
    product.price.amount = amount
    product.price.currency = "NOK"

    // This is messy code
    val gbs: Long = "${sku[0]}".toLong()
    product.properties = mapOf("noOfBytes" to "${gbs*1024*1024*1024}")
    product.presentation = mapOf("label" to "$gbs GB for ${amount/100}")

    return product
}