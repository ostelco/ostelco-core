package com.telenordigital.prime.storage.entities;

import com.telenordigital.prime.storage.Products;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ProductTest {

    private static final String SKU = "SKU-1";
    private static final String DESCRIPTION = "a random description";

    private final Product product = new Product(SKU, DESCRIPTION);

    @Test
    public void getSku() throws Exception {
        assertEquals(SKU, product.getSku());
    }


    @Test
    public void getProductDescription() throws Exception {
        assertEquals(DESCRIPTION, product.getProductDescription());
    }

    @Test(expected = NotATopupProductException.class)
    public void asTopupProductNot() throws Exception, NotATopupProductException {
        product.asTopupProduct();
    }



    public void asTopupProductTrue() throws Exception, NotATopupProductException {
        // Ghetto, not proper testing.
        assertTrue(Products.getProductForSku("DataTopup3GB").asTopupProduct()  instanceof  TopUpProduct);
    }


    @Test
    public void isTopUpProject() throws Exception {
        assertFalse(product.isTopUpProject());
    }

    @Test
    public void toStringTest() throws Exception {
        assertNotNull(product.toString());
    }
}
