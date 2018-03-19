package org.ostelco.prime.storage.entities


data class RecordOfPurchaseImpl(
        val _msisdn: String,
        val _sku: String,
        val _millisSinceEpoch: Long) : RecordOfPurchase {

    override val msisdn: String
        get() = _msisdn
    override val millisSinceEpoch: Long
        get() = _millisSinceEpoch
    override val sku: String
        get() = _sku
}
