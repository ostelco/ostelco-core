package org.ostelco.prime.storage.entities;

import lombok.Data;

import static com.google.common.base.Preconditions.checkNotNull;

@Data
public final class PurchaseRequestImpl implements PurchaseRequest {

    private  String sku;

    private  String paymentToken;

    private  String msisdn;

    private  long millisSinceEpoch;

    private  String id;

    public PurchaseRequestImpl(
            final Product product,
            final String paymentToken) {
        this.sku = checkNotNull(product.getSku());
        this.paymentToken = checkNotNull(paymentToken);
    }

    // XXX Weirdly enough, this is necessary.
    public PurchaseRequestImpl(){}
}
