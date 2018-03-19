package org.ostelco.prime.storage.entities

import org.junit.Test
import org.ostelco.prime.storage.Products

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

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

        private val SKU = "SKU-1"

        private val DESCRIPTION = "a random description"
    }
}
