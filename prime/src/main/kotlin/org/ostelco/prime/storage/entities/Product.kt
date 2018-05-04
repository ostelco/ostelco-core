package org.ostelco.prime.storage.entities

import org.ostelco.prime.model.Product
import org.ostelco.prime.model.TopUpProduct


fun Product.isTopUpProject(): Boolean = productDescription is TopUpProduct

/**
 * Return product as an instance of a TopUpProduct, or throw
 * an exception if it can't be cast into a TopUpProduct.
 * @return the product as a topup product, or throws an exception if
 * the product isn't a topup product.
 * @throws NotATopupProductException Thrown if the product if
 * not a topup product.
 */
@Throws(NotATopupProductException::class)
fun Product.asTopupProduct(): TopUpProduct? {
    try {
        return productDescription as TopUpProduct?
    } catch (ex: ClassCastException) {
        throw NotATopupProductException(ex)
    }
}