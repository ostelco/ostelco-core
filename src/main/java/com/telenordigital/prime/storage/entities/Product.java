package com.telenordigital.prime.storage.entities;

import lombok.Data;

@Data
public final class Product {

    /**
     * A "Stock Keeping Unit" that is assumed to be a primary key for products.
     */
    private final String sku;

    /**
     * A description intended to be useful for the consumer.
     */
    private final Object productDescription;
    
    /**
     * Return product as an instance of a TopUpProduct, or throw
     * an exception if it can't be cast into a TopUpProduct.
     * @return
     * @throws NotATopupProductException
     */
    public TopUpProduct asTopupProduct() throws NotATopupProductException{
        try {
            return (TopUpProduct) productDescription;
        } catch (ClassCastException ex) {
            throw new NotATopupProductException(ex);
        }

    }

    public boolean isTopUpProject() {
        return productDescription instanceof TopUpProduct;
    }
}
