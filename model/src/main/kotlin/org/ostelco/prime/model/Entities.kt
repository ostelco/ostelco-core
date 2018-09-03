package org.ostelco.prime.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.firebase.database.Exclude

interface HasId {
    val id: String
}

data class Offer(
        override val id: String,
        @JsonIgnore val segments: Collection<String> = emptyList(),
        @JsonIgnore val products: Collection<String> = emptyList()) : HasId

data class Segment(
        override val id: String,
        @JsonIgnore val subscribers: Collection<String> = emptyList()) : HasId

data class Subscriber(
        val email: String,
        val name: String = "",
        val address: String = "",
        val postCode: String = "",
        val city: String = "",
        val country: String = "",
        private val referralId: String = email) : HasId {

    constructor(email: String): this(email = email, referralId = email)

    fun getReferralId() = email

    override val id: String
        @JsonIgnore
        get() = email
}

// TODO vihang: make ApplicationToken data class immutable
// this data class is treated differently since it is stored in Firebase.
data class ApplicationToken(
        var token: String = "",
        var applicationID: String = "",
        var tokenType: String = "") : HasId {

    override val id: String
        @Exclude
        @JsonIgnore
        get() = applicationID
}

data class Subscription(
        val msisdn: String) : HasId {

    override val id: String
        @JsonIgnore
        get() = msisdn
}

data class Bundle(
        override val id: String,
        val balance: Long) : HasId

data class Price(
        val amount: Int,
        val currency: String)

data class Product(
        val sku: String,
        val price: Price,
        val properties: Map<String, String> = emptyMap(),
        val presentation: Map<String, String> = emptyMap()) : HasId {

    override val id: String
        @JsonIgnore
        get() = sku
}

data class ProductClass(
        override val id: String,
        val properties: List<String> = listOf()) : HasId

data class PurchaseRecord(
        override val id: String,
        @Deprecated("Will be removed in future") val msisdn: String,
        val product: Product,
        val timestamp: Long) : HasId

data class PseudonymEntity(
        val msisdn: String,
        val pseudonym: String,
        val start: Long,
        val end: Long)

data class ActivePseudonyms(
        val current: PseudonymEntity,
        val next: PseudonymEntity)