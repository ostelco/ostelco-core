package com.telenordigital.prime.storage;

import com.telenordigital.prime.storage.entities.Product;
import com.telenordigital.prime.storage.entities.TopUpProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkNotNull;


public final class  Products {

    public static final Product DATA_TOPUP_3GB =
            new Product("DataTopup3GB", new TopUpProduct(3000000000L));

    private static final  Logger LOG = LoggerFactory.getLogger(Products.class);

    private static final Map<String, Product> PRODUCTS;

    static {
        PRODUCTS = new TreeMap<>();
        PRODUCTS.put(DATA_TOPUP_3GB.getSku(), DATA_TOPUP_3GB);
    }

    /**
     * Utility class shouldn't have public constructor.
     */
    private Products() {}
    
    public static void addProduct(final Product p) {
        checkNotNull(p);
        PRODUCTS.put(p.getSku(), p);
    }

    public static void  addTopupProduct(final String sku, final long noOfBytes) {
        final Product topupProduct = newTopupProduct(sku, noOfBytes);
        LOG.info("Adding topup product  " + topupProduct);
        addProduct(topupProduct);
    }

    private static Product newTopupProduct(final String sku, final long noOfBytes) {
        return new Product(sku, new TopUpProduct(noOfBytes));
    }

    public static boolean isValidSKU(final String sku) {
        checkNotNull(sku);
        return PRODUCTS.containsKey(sku);
    }

    public static Product getProductForSku(final String sku) {
        checkNotNull(sku);
        return PRODUCTS.get(sku);
    }
}
