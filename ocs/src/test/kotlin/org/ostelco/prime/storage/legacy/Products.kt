package org.ostelco.prime.storage.legacy

import com.google.common.base.Preconditions.checkNotNull
import org.ostelco.prime.logger
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.TopUpProduct
import java.util.*


object Products {

    private val LOG by logger()

    val DATA_TOPUP_3GB = Product("DataTopup3GB", TopUpProduct(3000000000L))

    private val PRODUCTS: MutableMap<String, Product>

    init {
        PRODUCTS = TreeMap()
        PRODUCTS[DATA_TOPUP_3GB.sku] = DATA_TOPUP_3GB
    }

    fun addProduct(p: Product) {
        checkNotNull(p)
        PRODUCTS[p.sku] = p
    }

    fun addTopupProduct(sku: String, noOfBytes: Long) {
        val topupProduct = newTopupProduct(sku, noOfBytes)
        LOG.info("Adding topup product  " + topupProduct)
        addProduct(topupProduct)
    }

    private fun newTopupProduct(sku: String, noOfBytes: Long): Product {
        return Product(sku, TopUpProduct(noOfBytes))
    }

    fun isValidSKU(sku: String): Boolean {
        checkNotNull(sku)
        return PRODUCTS.containsKey(sku)
    }

    fun getProductForSku(sku: String): Product? {
        checkNotNull(sku)
        return PRODUCTS[sku]
    }
}
