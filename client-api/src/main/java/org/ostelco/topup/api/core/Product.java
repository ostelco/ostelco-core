package org.ostelco.topup.api.core;

import lombok.Data;
import lombok.NonNull;

@Data
public class Product {
    @NonNull private final String sku;
    private final float amount;
    @NonNull private final String currency;
}
