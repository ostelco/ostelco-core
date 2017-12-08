package com.telenordigital.prime.events;

import static com.google.common.base.Preconditions.checkNotNull;


// XXX Do the singelton thing.  It's ugly, but for now that's ok.

public final class Product {
    private final String sku;
    private final Object productDescription;


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
        return "Product{" +
                "sku='" + sku + '\'' +
                ", productDescription=" + productDescription +
                '}';
    }
}
