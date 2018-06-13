package org.ostelco.prime.model

import com.fasterxml.jackson.annotation.JsonIgnore

interface Entity {
    var id: String
}

data class Offer(
        @JsonIgnore
        override var id: String = "",
        var segments: List<Segment> = emptyList(),
        var products: List<Product> = emptyList()) : Entity

data class Segment(
        @JsonIgnore
        override var id: String = "",
        var subscribers: List<Subscriber> = emptyList()) : Entity

data class Subscriber(
        var email: String = "",
        var name: String = "",
        var address: String = "",
        var postCode: String = "",
        var city: String = "",
        var country: String = "") : Entity {

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
        var tokenType: String = "") : Entity {

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

data class Price(
        var amount: Int = 0,
        var currency: String = "")

data class Product(
        var sku: String = "",
        var price: Price = Price(0, ""),
        var properties: Map<String, String> = mapOf(),
        var presentation: Map<String, String> = mapOf()) : Entity {

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
        var properties: List<String> = listOf()) : Entity

data class PurchaseRecord(
        var msisdn: String = "",
        var product: Product = Product(),
        var timestamp: Long = 0L)

data class PseudonymEntity(
        var msisdn: String,
        var pseudonym: String,
        var start: Long,
        var end: Long)

data class ActivePseudonyms(
        var current: PseudonymEntity,
        var next: PseudonymEntity)
