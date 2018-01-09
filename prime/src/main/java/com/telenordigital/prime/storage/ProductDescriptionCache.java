package com.telenordigital.prime.storage;

import com.telenordigital.prime.storage.entities.Product;

public interface ProductDescriptionCache {
    void  addTopupProduct(String sku, long noOfBytes);

    boolean isValidSKU(String sku);

    Product getProductForSku(String sku);
}
