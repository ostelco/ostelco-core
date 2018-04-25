package org.ostelco.prime.storage.entities

import com.google.common.base.Preconditions.checkNotNull

class PurchaseRequestImpl() : PurchaseRequest {

    private var _sku: String? = null
    private var _paymentToken: String? = null
    private var _msisdn: String? = null
    private var _millisSinceEpoch: Long = 0
    private var _id: String? = null

    constructor(
            product: Product,
            paymentToken: String,
            msisdn: String?) : this() {
        this._sku = checkNotNull(product.sku)
        this._paymentToken = checkNotNull(paymentToken)
        this._msisdn = msisdn
    }

    override val sku: String
        get() = _sku ?: throw IncompletePurchaseRequestException()
    override val paymentToken: String
        get() = _paymentToken ?: throw IncompletePurchaseRequestException()
    override val msisdn: String
        get() = _msisdn ?: throw IncompletePurchaseRequestException()
    override val millisSinceEpoch: Long
        get() = _millisSinceEpoch
    override val id: String
        get() = _id ?: throw IncompletePurchaseRequestException()

    fun setMsisdn(msisdn: String) {
        this._msisdn = msisdn
    }

    fun setSku(sku: String) {
        this._sku = sku
    }

    fun setPaymentToken(paymentToken: String) {
        this._paymentToken = paymentToken
    }

    fun setMillisSinceEpoch(millisSinceEpoch: Long) {
        this._millisSinceEpoch = millisSinceEpoch
    }

    fun setId(id: String) {
        this._id = id
    }
}
