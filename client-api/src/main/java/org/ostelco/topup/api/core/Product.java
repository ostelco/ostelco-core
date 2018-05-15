package org.ostelco.topup.api.core;

import lombok.Data;

@Data
public class Product {
    private final String sku;
    private final float amount;
    private final String currency;
}
