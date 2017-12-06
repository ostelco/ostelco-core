package com.telenordigital.prime.events;
import com.google.cloud.TransportOptions;
import com.telenordigital.prime.firebase.InnerFbStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public final class  Products {
    private final static Logger LOG = LoggerFactory.getLogger(Products.class);

    public final static Product DATA_TOPUP_3GB =
            new Product("DataTopup3GB", new TopUpProduct(3000000000l));

    private static final Map<String, Product> products;

    static {
        products = new TreeMap<>();
        products.put(DATA_TOPUP_3GB.getSku(), DATA_TOPUP_3GB);
    }

    public static void addProduct(final Product p) {
        checkNotNull(p);
        products.put(p.getSku(), p);
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
        return products.containsKey(sku);
    }

    public static Product getProductForSku(final String sku) {
        checkNotNull(sku);
        return products.get(sku);
    }
}
