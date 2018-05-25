package org.ostelco.prime.storage.legacy

import org.ostelco.prime.model.Price
import org.ostelco.prime.model.Product
import java.util.*


object Products {

    val DATA_TOPUP_3GB = Product("DataTopup3GB",
            Price(30000, "NOK"),
            mapOf(Pair("noOfBytes", "${3L*1024*1024*1024}")),
            emptyMap())

    private val PRODUCTS: MutableMap<String, Product>

    init {
        PRODUCTS = TreeMap()
        PRODUCTS[DATA_TOPUP_3GB.sku] = DATA_TOPUP_3GB
    }

    fun getProduct(sku: String): Product? {
        return PRODUCTS[sku]
    }
}
