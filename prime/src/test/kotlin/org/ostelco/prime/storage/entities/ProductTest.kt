package org.ostelco.prime.storage.entities

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.TopUpProduct
import org.ostelco.prime.storage.Products

class ProductTest {

    private val product = Product(SKU, DESCRIPTION)


    @Test(expected = NotATopupProductException::class)
    @Throws(NotATopupProductException::class)
    fun asTopupProductNot() {
        product.asTopupProduct()
    }

    @Throws(NotATopupProductException::class)
    fun asTopupProductTrue() {
        // Ghetto, not proper testing.
        assertTrue(Products.getProductForSku("DataTopup3GB")!!.asTopupProduct() is TopUpProduct)
    }

    @Test
    fun isTopUpProject() {
        assertFalse(product.isTopUpProject())
    }

    companion object {

        private const val SKU = "SKU-1"

        private const val DESCRIPTION = "a random description"
    }
}
