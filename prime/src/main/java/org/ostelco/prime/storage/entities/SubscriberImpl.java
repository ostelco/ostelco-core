package org.ostelco.prime.storage.entities;

import lombok.Data;


@Data
public final class SubscriberImpl implements Subscriber {

    private String fbKey;

    private String msisdn;

    private long noOfBytesLeft;
}
