package org.ostelco.topup.api.core;

import lombok.Data;
import lombok.NonNull;
import org.ostelco.prime.client.api.model.Product;

import java.util.List;

@Data
public class SubscriptionStatus {
    private final int remaining;
    @NonNull private final List<Product> acceptedProducts;
}
