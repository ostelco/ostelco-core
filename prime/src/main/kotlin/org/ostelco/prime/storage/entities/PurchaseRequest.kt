package org.ostelco.prime.storage.entities

import java.util.*

interface PurchaseRequest {
    val sku: String

    val paymentToken: String

    val msisdn: String

    val millisSinceEpoch: Long

    val id: String

    fun asMap(): Map<String, Any> {
        val result = HashMap<String, Any>()
        result["msisdn"] = msisdn
        result["sku"] = sku
        result["paymentToken"] = paymentToken
        return result
    }
}
