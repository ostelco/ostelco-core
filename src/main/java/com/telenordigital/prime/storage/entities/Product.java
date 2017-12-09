package com.telenordigital.prime.storage.entities;

import static com.google.common.base.Preconditions.checkNotNull;


// XXX Do the singelton thing.  It's ugly, but for now that's ok.

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
     *
     * @param sku A "Stock Keeping Unit" that is assumed to be a primary key for products.
     * @param productDescription A description intended to be useful for the consumer.
     */
    public Product(final String sku, final Object productDescription) {
        this.sku = checkNotNull(sku);
        this.productDescription = checkNotNull(productDescription);
    }

    public String getSku() {
        return sku;
    }

    public Object getProductDescription() {
        return productDescription;
    }

    public TopUpProduct asTopupProduct() {
        return (TopUpProduct) productDescription;
    }

    public boolean isTopUpProject() {
        return (productDescription instanceof TopUpProduct);
    }


    @Override
    public String toString() {
        return "Product{"
                + "sku='" + sku + '\''
                + ", productDescription=" + productDescription
                + '}';
    }
}
