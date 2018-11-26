package org.ostelco.prime.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.firebase.database.Exclude

interface HasId {
    val id: String
}

data class Offer(
        override val id: String,
        val segments: Collection<String> = emptyList(),
        val products: Collection<String> = emptyList()) : HasId

data class Segment(
        override val id: String,
        val subscribers: Collection<String> = emptyList()) : HasId

data class ChangeSegment(
        val sourceSegmentId: String,
        val targetSegmentId: String,
        val subscribers: Collection<String>)

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
        val msisdn: String,
        val alias: String = "") : HasId {

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

// Note: The 'name' value becomes the name (sku) of the corresponding product in Stripe.
data class Plan(
        val name: String,
        val price: Price,
        val interval: String,
        val intervalCount: Long = 1L,
        val planId: String = "") : HasId {

    override val id: String
        @JsonIgnore
        get() = name
}

data class RefundRecord(
        override val id: String,
        val reason: String, // possible values are duplicate, fraudulent, and requested_by_customer
        val timestamp: Long) : HasId

data class PurchaseRecord(
        override val id: String,
        @Deprecated("Will be removed in future") val msisdn: String,
        val product: Product,
        val timestamp: Long,
        val refund: RefundRecord? = null) : HasId

data class PurchaseRecordInfo(override val id: String,
                              val subscriberId: String,
                              val product: Product,
                              val timestamp: Long,
                              val status: String) : HasId {
    constructor(purchaseRecord: PurchaseRecord, subscriberId: String, status: String) : this(
            purchaseRecord.id,
            subscriberId,
            purchaseRecord.product,
            purchaseRecord.timestamp,
            status)
}

data class PseudonymEntity(
        val sourceId: String,
        val pseudonym: String,
        val start: Long,
        val end: Long)

data class ActivePseudonyms(
        val current: PseudonymEntity,
        val next: PseudonymEntity)
