package com.telenordigital.prime.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public final class ProductDescriptionCacheImpl implements ProductDescriptionCache {

    private static final Logger LOG = LoggerFactory.getLogger(ProductDescriptionCacheImpl.class);

    public static final Product DATA_TOPUP_3GB =
            new Product("DataTopup3GB", new TopUpProduct(3000000000L));

    private static final ProductDescriptionCacheImpl INSTANCE = new ProductDescriptionCacheImpl();

    private final Map<String, Product> products;

    public static ProductDescriptionCacheImpl getInstance() {
        return INSTANCE;
    }


    private ProductDescriptionCacheImpl() {
        products = new TreeMap<>();
        products.put(DATA_TOPUP_3GB.getSku(), DATA_TOPUP_3GB);
    }

    public synchronized void addProduct(final Product p) {
        checkNotNull(p);
        products.put(p.getSku(), p);
    }

    @Override
    public void addTopupProduct(final String sku, final long noOfBytes) {
        checkNotNull(sku);
        checkArgument(noOfBytes >= 0);
        final Product topupProduct = newTopupProduct(sku, noOfBytes);
        LOG.info("Adding topup product  " + topupProduct);
        addProduct(topupProduct);
    }

    private synchronized Product newTopupProduct(final String sku, final long noOfBytes) {
        checkNotNull(sku);
        checkArgument(noOfBytes >= 0);
        return new Product(sku, new TopUpProduct(noOfBytes));
    }

    @Override
    public synchronized boolean isValidSKU(final String sku) {
        checkNotNull(sku);
        return products.containsKey(sku);
    }

    @Override
    public synchronized Product getProductForSku(final String sku) {
        checkNotNull(sku);
        return products.get(sku);
    }
}
