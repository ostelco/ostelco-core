package org.ostelco.prime.storage;


import org.ostelco.prime.storage.entities.Product;

public interface ProductDescriptionCache {
    void  addTopupProduct(String sku, long noOfBytes);

    boolean isValidSKU(String sku);

    Product getProductForSku(String sku);
}
