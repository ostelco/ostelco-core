package org.ostelco.topup.api.core;

import lombok.Data;

@Data
public class Consent {
    private final String consentId;
    private final String description;
    private final boolean accepted;
}
