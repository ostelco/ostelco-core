package com.telenordigital.prime.storage;

import lombok.Data;

/**
 * Bean used to use read items from the product catalog.
 */
@Data
public final class ProductCatalogItem {

    private String badgeLabel;

    private String currencyLabel;

    private boolean isVisible;

    private String label;

    private int price;

    private String priceLabel;

    private int amount;

    private String sku;

    private long noOfBytes;
}
