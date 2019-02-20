package org.ostelco.prime.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.cloud.datastore.Blob
import com.google.firebase.database.Exclude
import java.util.*

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

data class Customer(
        override val id: String = UUID.randomUUID().toString(),
        val email: String,
        val name: String = "",
        val address: String = "",
        val postCode: String = "",
        val city: String = "",
        val country: String = "",
        val referralId: String = UUID.randomUUID().toString()) : HasId

data class Identity(
        override val id: String,
        val type: String,
        val provider: String) : HasId

enum class CustomerStatus {
    REGISTERED,         // User has registered an account, eKYC results are pending
    EKYC_REJECTED,      // eKYC documents were rejected
    EKYC_APPROVED,      // eKYC documents were approved
    ACTIVE,             // Subscriber is active
    INACTIVE            // Inactive customer
}

data class CustomerState(
        val status: CustomerStatus,   // Current status of the customer
        val modifiedTimestamp: Long,    // last modification time of the customer status
        val scanId: String?,            // id of the last successful scan.
        override val id: String
) : HasId

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
        val rejectReason: String?)

data class ScanInformation(
        val scanId: String,
        val countryCode: String,
        val status: ScanStatus,
        val scanResult: ScanResult?
) : HasId {

    override val id: String
        @Exclude
        @JsonIgnore
        get() = scanId
}

data class VendorScanInformation(
        val id: String,                 // Id of the scan
        val scanReference: String,      // Jumio transaction reference
        val details: String,            // JSON string representation of all the information from vendor
        val images: Map<String, Blob>?  // liveness images (JPEG or PNG) if available
)


data class ScanMetadata(
        val id: String,                 // Id of the scan
        val scanReference: String,      // Jumio transaction reference
        val countryCode: String,        // The country for which the scan was done
        val customerId: String,         // The owner of the scan
        val processedTime: Long         // The time when callback was processed.
)

enum class ScanMetadataEnum(val s: String) {
    // Property names for Datastore
    ID("id"),
    SCAN_REFERENCE("scanReference"),
    COUNTRY_CODE("countryCode"),
    CUSTOMER_ID("customerId"),
    PROCESSED_TIME("processedTime"),
    // Type name of the Object
    KIND("ScanMetaData")
}

enum class JumioScanData(val s: String) {
    // Property names in POST data from Jumio
    JUMIO_SCAN_ID("jumioIdScanReference"),
    SCAN_ID("merchantIdScanReference"),
    SCAN_STATUS("idScanStatus"),
    VERIFICATION_STATUS("verificationStatus"),
    CALLBACK_DATE("callbackDate"),
    ID_TYPE("idType"),
    ID_COUNTRY("idCountry"),
    ID_FIRSTNAME("idFirstName"),
    ID_LASTNAME("idLastName"),
    ID_DOB("idDob"),
    SCAN_IMAGE("idScanImage"),
    SCAN_IMAGE_FACE("idScanImageFace"),
    SCAN_IMAGE_BACKSIDE("idScanImageBackside"),
    SCAN_LIVENESS_IMAGES("livenessImages"),
    REJECT_REASON("rejectReason"),
    IDENTITY_VERIFICATION("identityVerification"),
    SIMILARITY("similarity"),
    VALIDITY("validity"),
    APPROVED_VERIFIED("APPROVED_VERIFIED"),
    MATCH("MATCH"),
    TRUE("TRUE")
}

enum class VendorScanData(val s: String) {
    // Property names in VendorScanInformation
    ID("scanId"),
    DETAILS("scanDetails"),
    IMAGE("scanImage"),
    IMAGE_TYPE("scanImageType"),
    IMAGEBACKSIDE("scanImageBackside"),
    IMAGEBACKSIDE_TYPE("scanImageBacksideType"),
    IMAGEFACE("scanImageFace"),
    IMAGEFACE_TYPE("scanImageFaceType"),
    // Name of the datastore type
    TYPE_NAME("VendorScanInformation")
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