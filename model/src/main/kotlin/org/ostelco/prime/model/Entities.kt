package org.ostelco.prime.model

import com.fasterxml.jackson.annotation.JsonIgnore

interface HasId {
    var id: String
}

open class Entity : HasId {
    @JsonIgnore
    override var id: String = ""
}

data class Offer(
        var segments: Collection<String> = emptyList(),
        var products: Collection<String> = emptyList()) : Entity()

data class Segment(
        var subscribers: Collection<String> = emptyList()) : Entity()

data class Subscriber(
        var email: String = "",
        var name: String = "",
        var address: String = "",
        var postCode: String = "",
        var city: String = "",
        var country: String = "",
        var referralId: String = email) : HasId {

    constructor(email: String) : this() {
        this.email = email
    }

    override var id: String
        @JsonIgnore
        get() = email
        @JsonIgnore
        set(value) {
            email = value
        }
}

data class ApplicationToken(
        var token: String = "",
        var applicationID: String = "",
        var tokenType: String = "") : HasId {

    constructor(applicationID: String) : this() {
        this.applicationID = applicationID
    }

    override var id: String
        @JsonIgnore
        get() = applicationID
        @JsonIgnore
        set(value) {
            applicationID = value
        }
}

data class Subscription(
        var msisdn: String = "") : HasId {

    override var id: String
        @JsonIgnore
        get() = msisdn
        @JsonIgnore
        set(value) {
            msisdn = value
        }
}

data class Bundle(
        override var id: String = "",
        var balance: Long = 0) : HasId

data class Price(
        var amount: Int = 0,
        var currency: String = "")

data class Product(
        var sku: String = "",
        var price: Price = Price(0, ""),
        var properties: Map<String, String> = mapOf(),
        var presentation: Map<String, String> = mapOf()) : HasId {

    override var id: String
        @JsonIgnore
        get() = sku
        @JsonIgnore
        set(value) {
            sku = value
        }
}

data class ProductClass(
        override var id: String = "",
        var properties: List<String> = listOf()) : HasId

data class PurchaseRecord(
        override var id: String = "",
        @Deprecated("Will be removed in future") var msisdn: String = "",
        var product: Product = Product(),
        var timestamp: Long = 0L) : HasId

data class PseudonymEntity(
        var msisdn: String,
        var pseudonym: String,
        var start: Long,
        var end: Long)

data class ActivePseudonyms(
        var current: PseudonymEntity,
        var next: PseudonymEntity)