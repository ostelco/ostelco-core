package org.ostelco.topup.api.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@AllArgsConstructor
@Data
public class SubscriptionStatus {
    private final int remaining;
    private final List<AcceptedOffer> acceptedOffers;
}
