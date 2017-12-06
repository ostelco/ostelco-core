package com.telenordigital.prime.events;

public interface ProductDescriptionCache {
    void  addTopupProduct(String sku, long noOfBytes);

    boolean isValidSKU(String sku);

    Product getProductForSku(String sku);
}
