package com.telenordigital.prime.storage.entities;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProductTest {

    private final static String SKU = "SKU-1";
    private final static String DESCRIPTION = "a random description";

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
    public void asTopupProduct() throws Exception, NotATopupProductException {
        product.asTopupProduct();
    }

    // XXX Should also try to actually get a topop product.


    @Test
    public void isTopUpProject() throws Exception {
        assertFalse(product.isTopUpProject());
    }

    @Test
    public void toStringTest() throws Exception {
        assertNotNull(product.toString());
    }
}