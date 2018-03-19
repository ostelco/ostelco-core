package org.ostelco.prime.storage.entities

data class Product(
    /**
     * A "Stock Keeping Unit" that is assumed to be a primary key for products.
     */
    val sku: String,
    /**
     * A description intended to be useful for the consumer.
     */
    val productDescription: Any) {

    fun isTopUpProject(): Boolean = productDescription is TopUpProduct

    /**
     * Return product as an instance of a TopUpProduct, or throw
     * an exception if it can't be cast into a TopUpProduct.
     * @return the product as a topup product, or throws an exception if
     * the product isn't a topup product.
     * @throws NotATopupProductException Thrown if the product if
     * not a topup product.
     */
    @Throws(NotATopupProductException::class)
    fun asTopupProduct(): TopUpProduct? {
        try {
            return productDescription as TopUpProduct?
        } catch (ex: ClassCastException) {
            throw NotATopupProductException(ex)
        }

    }
}
