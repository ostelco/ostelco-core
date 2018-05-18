package org.ostelco.topup.api.core;

import lombok.Data;
import lombok.NonNull;
import java.util.List;

@Data
public class SubscriptionStatus {
    private final int remaining;
    @NonNull private final List<Product> acceptedProducts;
}
