package org.ostelco.prime.storage.entities;

import org.junit.Test;
import org.ostelco.prime.storage.Products;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ProductTest {

    private static final String SKU = "SKU-1";

    private static final String DESCRIPTION = "a random description";

    private final Product product = new Product(SKU, DESCRIPTION);


    @Test(expected = NotATopupProductException.class)
    public void asTopupProductNot() throws NotATopupProductException {
        product.asTopupProduct();
    }

    public void asTopupProductTrue() throws NotATopupProductException {
        // Ghetto, not proper testing.
        assertTrue(Products.getProductForSku("DataTopup3GB").asTopupProduct()
                instanceof  TopUpProduct);
    }

    @Test
    public void isTopUpProject() {
        assertFalse(product.isTopUpProject());
    }
}
