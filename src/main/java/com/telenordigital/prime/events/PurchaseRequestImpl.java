package com.telenordigital.prime.events;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

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

    public PurchaseRequestImpl(){}

    @Override
    public String getMsisdn() {
        return msisdn;
    }

    @Override
    public String getSku() {
        return sku;
    }

    public void setSku(final String sku) {
        this.sku = checkNotNull(sku);
    }

    @Override
    public String getPaymentToken() {
        return paymentToken;
    }

    public void setPaymentToken(final String paymentToken) {
        this.paymentToken = checkNotNull(paymentToken);
    }

    public void setMsisdn(final String msisdn) {
        this.msisdn = msisdn;
    }

    @Override
    public long getMillisSinceEpoch() {
        return millisSinceEpoch;
    }

    public void setMillisSinceEpoch(final long millisSinceEpoch) {
        this.millisSinceEpoch = millisSinceEpoch;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }


    public Map<String, Object> asMap() {
        final Map<String, Object> result = new HashMap<>();
        result.put("msisdn", msisdn);
        result.put("sku", sku);
        result.put("paymentToken", paymentToken);
        return result;
    }

    @Override
    public String toString() {
        return "PurchaseRequestImpl{"
                + "sku='" + sku + '\''
                + ", paymentToken='" + paymentToken + '\''
                + ", msisdn='" + msisdn + '\''
                + ", millisSinceEpoch=" + millisSinceEpoch
                + '}';
    }
}
