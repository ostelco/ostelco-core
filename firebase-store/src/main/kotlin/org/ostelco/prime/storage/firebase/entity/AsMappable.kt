package org.ostelco.prime.storage.firebase.entity

import org.ostelco.prime.model.PurchaseRequest
import org.ostelco.prime.model.RecordOfPurchase
import org.ostelco.prime.model.Subscriber

/**
 * Returns a map that is intended to be written to a Firebase subtree.
 *
 */

fun Subscriber.asMap(): Map<String, Any?> {
    val result = HashMap<String, Any?>()
    result["noOfBytesLeft"] = noOfBytesLeft
    result["msisdn"] = msisdn
    return result
}

fun RecordOfPurchase.asMap(): Map<String, Any> {
    val result = HashMap<String, Any>()
    result["msisdn"] = msisdn
    result["sku"] = sku
    result["timeInMillisSinceEpoch"] = millisSinceEpoch
    return result
}

fun PurchaseRequest.asMap(): Map<String, Any> {
    val result = HashMap<String, Any>()
    result["msisdn"] = msisdn
    result["sku"] = sku
    result["paymentToken"] = paymentToken
    return result
}