package org.ostelco.topup.api.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AcceptedOffer {
    private final String offerId;
    private final int value;
    private final int usage;
    private final long expires;
}
