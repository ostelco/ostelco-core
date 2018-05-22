package org.ostelco.prime.storage.firebase.entity

class FbSubscriber() {
    var msisdn: String? = null
    var noOfBytesLeft: Long = 0
}

class FbPurchaseRequest() {
    var sku: String = ""
    var paymentToken: String = ""
    var msisdn: String = ""
    var millisSinceEpoch: Long = 0
    var id: String = ""
}