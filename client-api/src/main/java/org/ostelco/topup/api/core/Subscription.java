package org.ostelco.topup.api.core;

import lombok.Data;
import lombok.NonNull;

@Data
public class Subscription {
    private final String subscriptionId;
    private final String name;
    private final String email;
    private final String msisdn;
    private final String imsi;
}
