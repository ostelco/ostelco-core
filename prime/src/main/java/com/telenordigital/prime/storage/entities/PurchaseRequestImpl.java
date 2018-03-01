package com.telenordigital.prime.storage.entities;

import lombok.Data;

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
