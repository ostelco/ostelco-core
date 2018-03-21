package org.ostelco.prime.storage.entities;

import lombok.Data;
import lombok.Getter;

import static com.google.api.client.util.Preconditions.checkNotNull;

@Data
public final class RecordOfPurchaseImpl implements RecordOfPurchase {

    @Getter private  final String msisdn;

    @Getter private  final String sku;

    @Getter private  final long millisSinceEpoch;

    public RecordOfPurchaseImpl(
            final String msisdn,
            final String sku,
            final long millisSinceEpoch) {
        this.msisdn = checkNotNull(msisdn);
        this.sku = checkNotNull(sku);
        this.millisSinceEpoch = millisSinceEpoch;
    }
}
