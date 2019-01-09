package org.ostelco.prime.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.cloud.datastore.Blob
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


enum class SubscriberStatus {
    REGISTERED,         // User has registered an account, eKYC results are pending
    EKYC_REJECTED,      // eKYC documents were rejected
    EKYC_APPROVED,      // eKYC documents were approved
    ACTIVE,             // Subscriber is active
    INACTIVE            // Inactive subscriber
}

data class SubscriberState(
        val status: SubscriberStatus,   // Current status of the subscriber
        val modifiedTimestamp: Long,    // last modification time of the subscriber status
        override val id: String
): HasId

enum class ScanStatus {
    PENDING,        // scan results are pending
    REJECTED,       // scanned Id was rejected
    APPROVED        // scanned Id was approved
}

data class ScanResult(
        val vendorScanReference: String,
        val verificationStatus: String,
        val time: Long,
        val type: String?,
        val country: String?,
        val firstName: String?,
        val lastName: String?,
        val dob: String?,
        val rejectReason: String?
)

data class ScanInformation(
        val scanId:String,
        val status: ScanStatus,
        val scanResult: ScanResult?
) : HasId {

    override val id: String
        @Exclude
        @JsonIgnore
        get() = scanId
}

data class VendorScanInformation(
        val scanId: String,                     // Id of the scan
        val scanDetails: String,                // JSON string representation of all the information from vendor
        val scanImage: Blob,                    // image of the scan (JPEG or PNG)
        val scanImageType: String,              // type of image (e.g. image/jpg)
        val scanImageBackside: Blob?,           // back side image of the scan (JPEG or PNG) if available
        val scanImageBacksideType: String?,     // type of back side image (e.g. image/jpg)
        val scanImageFace: Blob?,               // face image of the scan (JPEG or PNG) if available
        val scanImageFaceType: String?          // type of face image (e.g. image/jpg)
)

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
        val properties: Map<String, String> = emptyMap(),
        val presentation: Map<String, String> = emptyMap()) : HasId {

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
