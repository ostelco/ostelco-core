package org.ostelco.prime.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.cloud.datastore.Blob
import com.google.firebase.database.Exclude
import org.ostelco.prime.model.PaymentProperties.LABEL
import org.ostelco.prime.model.PaymentProperties.TAX_REGION_ID
import org.ostelco.prime.model.ProductProperties.NO_OF_BYTES
import org.ostelco.prime.model.ProductProperties.PRODUCT_CLASS
import org.ostelco.prime.model.ProductProperties.SEGMENT_IDS
import java.util.*


interface HasId {
    val id: String
}

data class Region(
        override val id: String,
        val name: String) : HasId {

    companion object
}

data class Offer(
        val id: String,
        val segments: Collection<String> = emptyList(),
        val products: Collection<String> = emptyList())

data class Segment(
        val id: String,
        val subscribers: Collection<String> = emptyList())

data class ChangeSegment(
        val sourceSegmentId: String,
        val targetSegmentId: String,
        val subscribers: Collection<String>)

data class Customer(
        override val id: String = UUID.randomUUID().toString(),
        val nickname: String,
        val contactEmail: String,
        val analyticsId: String = UUID.randomUUID().toString(),
        val referralId: String = UUID.randomUUID().toString()) : HasId {

    companion object
}

data class Identity(
        val id: String,
        val type: String,
        val provider: String)

data class RegionDetails(
        val region: Region,
        val status: CustomerRegionStatus,
        val kycStatusMap: Map<KycType, KycStatus> = emptyMap(),
        val simProfiles: Collection<SimProfile> = emptyList())

enum class CustomerRegionStatus {
    PENDING,   // eKYC initiated, but not yet approved
    APPROVED,  // eKYC approved
    AVAILABLE  // Region is available for provisioning
}

enum class KycType {
    JUMIO,
    MY_INFO,
    NRIC_FIN,
    ADDRESS
}

enum class KycStatus {
    PENDING,   // eKYC initiated, but not yet approved or rejected
    REJECTED,  // eKYC rejected
    APPROVED   // eKYC approved
}

data class MyInfoConfig(val url: String)

enum class MyInfoApiVersion {
    V2,
    V3
}

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
        val handwrittenNoteMatches: Boolean?
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

    companion object
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
    JUMIO_NOTIFICATION_TITLE("eKYC Status"),
    JUMIO_IDENTITY_VERIFIED("Successfully verified the identity"),
    JUMIO_IDENTITY_FAILED("Failed to verify the identity"),
    SUBSCRIPTION_RENEWAL_TITLE("Subscription Renewal"),
    SUBSCRIPTION_PAYMENT_METHOD_REQUIRED("Subscription renewal failed due to missing or invalid card. Please add a new card.")
}

data class ApplicationToken(
        val token: String,
        val applicationID: String,
        val tokenType: String) : HasId {

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

    companion object
}

data class Bundle(
        override val id: String,
        val balance: Long) : HasId {

    companion object
}

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

    val paymentLabel: String
        @Exclude
        @JsonIgnore
        get() = payment[LABEL.s] ?: sku

    val paymentTaxRegionId: String?
        @Exclude
        @JsonIgnore
        get() = payment[TAX_REGION_ID.s]

    // Values from Properties map

    val productClass: ProductClass?
        @Exclude
        @JsonIgnore
        get() = properties[PRODUCT_CLASS.s]?.let(ProductClass::valueOf)

    val noOfBytes: Long
        @Exclude
        @JsonIgnore
        get() = properties[NO_OF_BYTES.s]?.replace("_", "")?.toLongOrNull() ?: 0L

    val segmentIds: Collection<String>
        @Exclude
        @JsonIgnore
        get() = properties[SEGMENT_IDS.s]?.split(",") ?: emptyList()

    companion object
}

enum class ProductProperties(val s: String) {
    PRODUCT_CLASS("productClass"),
    NO_OF_BYTES("noOfBytes"),
    SEGMENT_IDS("segmentIds")
}

enum class ProductClass {
    SIMPLE_DATA,
    SUBSCRIPTION,
    MEMBERSHIP
}

enum class PaymentProperties(val s: String) {
    LABEL("label"),
    TAX_REGION_ID("taxRegionId")
}

/* Notes:
   - The 'name' value becomes the name (sku) of the corresponding product
     in Stripe.
   - 'trialPeriod' is in milliseconds. Default is no trial period. */
data class Plan(
        override val id: String,
        val stripePlanId: String? = null,
        val stripeProductId: String? = null,
        val interval: String,
        val intervalCount: Long = 1L,
        val trialPeriod: Long = 0L) : HasId {

    companion object
}

data class RefundRecord(
        override val id: String,
        val reason: String, // possible values are duplicate, fraudulent, and requested_by_customer
        val timestamp: Long) : HasId

/* TODO! (kmm) To support prorating with subscriptions, the
         amount paid values should be taken from the invoice
         and not from the product class. */
data class PurchaseRecord(
        override val id: String,           /* 'charge' id. */
        val product: Product,
        val timestamp: Long,
        val refund: RefundRecord? = null,
        /* For storing 'invoice-id' when purchasing a plan. */
        val properties: Map<String, String> = emptyMap()) : HasId {

    companion object
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
        val regions: Collection<RegionDetails> = emptyList()
)

data class CustomerActivity(
        val timestamp: Long,
        val severity: Severity,
        val message: String
)

enum class Severity {
    INFO,
    WARN,
    ERROR
}
