package org.ostelco.prime.model

data class PurchaseRequest(
        var sku: String,
        var paymentToken: String,
        var msisdn: String,
        var millisSinceEpoch: Long,
        var id: String)

data class RecordOfPurchase(
        val msisdn: String,
        val sku: String,
        val millisSinceEpoch: Long)

data class Subscriber(
        val msisdn: String,
        val noOfBytesLeft: Long)

data class TopUpProduct(val noOfBytes: Long)

data class Product(
        /**
         * A "Stock Keeping Unit" that is assumed to be a primary key for products.
         */
        val sku: String,
        /**
         * A description intended to be useful for the consumer.
         */
        val productDescription: Any)