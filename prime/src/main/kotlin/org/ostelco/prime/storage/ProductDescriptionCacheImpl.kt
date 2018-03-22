package org.ostelco.prime.storage

import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Preconditions.checkNotNull
import org.ostelco.prime.logger
import org.ostelco.prime.storage.entities.Product
import org.ostelco.prime.storage.entities.TopUpProduct
import java.util.*


object ProductDescriptionCacheImpl : ProductDescriptionCache {

    private val LOG by logger()

    private val products: MutableMap<String, Product>
    val DATA_TOPUP_3GB = Product("DataTopup3GB", TopUpProduct(3000000000L))

    init {
        products = TreeMap()
        products[DATA_TOPUP_3GB.sku] = DATA_TOPUP_3GB
    }

    @Synchronized
    fun addProduct(p: Product) {
        checkNotNull(p)
        products[p.sku] = p
    }

    override fun addTopupProduct(sku: String, noOfBytes: Long) {
        checkNotNull(sku)
        checkArgument(noOfBytes >= 0)
        val topupProduct = newTopupProduct(sku, noOfBytes)
        LOG.info("Adding topup product  " + topupProduct)
        addProduct(topupProduct)
    }

    @Synchronized
    private fun newTopupProduct(sku: String, noOfBytes: Long): Product {
        checkNotNull(sku)
        checkArgument(noOfBytes >= 0)
        return Product(sku, TopUpProduct(noOfBytes))
    }

    @Synchronized
    override fun isValidSKU(sku: String): Boolean {
        checkNotNull(sku)
        return products.containsKey(sku)
    }

    @Synchronized
    override fun getProductForSku(sku: String): Product? {
        checkNotNull(sku)
        return products[sku]
    }
}
