package org.ostelco.topup.api.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Offer {
    private final String offerId;
    private final String label;
    private final float price;
    private final int value;
    private final long expires;
}
