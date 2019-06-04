package org.ostelco.prime.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.cloud.datastore.Blob
import com.google.firebase.database.Exclude
import java.util.*


interface HasId {
    val id: String
}

data class Region(
        override val id: String,
        val name: String) : HasId

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
        val nickname: String,
        val contactEmail: String,
        val analyticsId: String = UUID.randomUUID().toString(),
        val referralId: String = UUID.randomUUID().toString()) : HasId

data class Identity(
        override val id: String,
        val type: String,
        val provider: String) : HasId

data class RegionDetails(
        val region: Region,
        val status: CustomerRegionStatus,
        val kycStatusMap: Map<KycType, KycStatus> = emptyMap(),
        val simProfiles: Collection<SimProfile> = emptyList())

enum class CustomerRegionStatus {
    PENDING,   // eKYC initiated, but not yet approved
    APPROVED,  // eKYC approved
}

enum class KycType {
    JUMIO,
    MY_INFO,
    NRIC_FIN,
    ADDRESS_AND_PHONE_NUMBER
}

enum class KycStatus {
    PENDING,   // eKYC initiated, but not yet approved or rejected
    REJECTED,  // eKYC rejected
    APPROVED   // eKYC approved
}

data class MyInfoConfig(val url: String)

enum class ScanStatus {
    PENDING,   // scan results are pending
    REJECTED,  // scanned Id was rejected
    APPROVED   // scanned Id was approved
}

// Jumio Identity verification codes for Similarity
enum class Similarity {
    MATCH,
    NO_MATCH,
    NOT_POSSIBLE
}

// Jumio Identity verification reasons for validity being fasle
enum class ValidityReason {
    SELFIE_CROPPED_FROM_ID,
    ENTIRE_ID_USED_AS_SELFIE,
    MULTIPLE_PEOPLE,
    SELFIE_IS_SCREEN_PAPER_VIDEO,
    SELFIE_MANIPULATED,
    AGE_DIFFERENCE_TOO_BIG,
    NO_FACE_PRESENT,
    FACE_NOT_FULLY_VISIBLE,
    BAD_QUALITY,
    BLACK_AND_WHITE,
    LIVENESS_FAILED
}

// Jumio Identity verification structure, valid when a scan is verified & approved
data class IdentityVerification(
        val similarity: Similarity,
        @JsonDeserialize(using = StringBooleanDeserializer::class)
        val validity: Boolean,
        val reason: ValidityReason?,
        @JsonDeserialize(using = StringBooleanDeserializer::class)
        val handwrittenNoteMatches:Boolean?
)

data class ScanResult(
        val vendorScanReference: String,
        val verificationStatus: String,
        val time: Long,
        val type: String?,
        val country: String?,
        val firstName: String?,
        val lastName: String?,
        val dob: String?,
        val rejectReason: IdentityVerification?)

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
    APPROVED_VERIFIED("APPROVED_VERIFIED"),
    // Extended values from prime
    SCAN_INFORMATION("SCAN_INFORMATION")
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

enum class FCMStrings(val s: String) {
    NOTIFICATION_TITLE("eKYC Status"),
    JUMIO_IDENTITY_VERIFIED("Successfully verified the identity"),
    JUMIO_IDENTITY_FAILED("Failed to verify the identity")
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
        val analyticsId: String = UUID.randomUUID().toString()) : HasId {

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
        val payment: Map<String, String> = emptyMap(),
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
        val payment: Map<String, String> = emptyMap(),
        val interval: String,
        val intervalCount: Long = 1L,
        val segments: Collection<String> = emptyList(),
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
        override val id: String,           /* 'charge' id. */
        val product: Product,
        val timestamp: Long,
        val refund: RefundRecord? = null,
        /* For storing 'invoice-id' when purchasing a plan. */
        val properties: Map<String, String> = emptyMap()) : HasId

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

data class SimEntry(
        val iccId: String,
        val status: SimProfileStatus,
        val eSimActivationCode: String,
        val msisdnList: Collection<String>)

data class SimProfile(
        val iccId: String,
        @JvmField val eSimActivationCode: String,
        val status: SimProfileStatus,
        val alias: String = "")

enum class SimProfileStatus {
    NOT_READY,
    AVAILABLE_FOR_DOWNLOAD,
    DOWNLOADED,
    INSTALLED,
    ENABLED,
}

data class Context(
        val customer: Customer,
        val regions: Collection<RegionDetails> = emptyList())
