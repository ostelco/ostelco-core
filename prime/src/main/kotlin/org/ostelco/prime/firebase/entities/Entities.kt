package org.ostelco.prime.firebase.entities

class FbSubscriber()  {
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