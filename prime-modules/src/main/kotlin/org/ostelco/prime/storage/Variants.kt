package org.ostelco.prime.storage

import arrow.core.Either
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.ChangeSegment
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Plan
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Region
import org.ostelco.prime.model.RegionDetails
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.SimProfile
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import javax.ws.rs.core.MultivaluedMap

interface ClientDocumentStore {

    /**
     * Get token used for sending notification to user application
     */
    fun getNotificationTokens(customerId: String): Collection<ApplicationToken>

    /**
     * Add token used for sending notification to user application
     */
    fun addNotificationToken(customerId: String, token: ApplicationToken): Boolean

    /**
     * Get token used for sending notification to user application
     */
    fun getNotificationToken(customerId: String, applicationID: String): ApplicationToken?

    /**
     * Get token used for sending notification to user application
     */
    fun removeNotificationToken(msisdn: String, applicationID: String): Boolean

    fun getPaymentId(id: String): String?

    fun deletePaymentId(id: String): Boolean

    fun createPaymentId(id: String, paymentId: String): Boolean
}

interface AdminDocumentStore

interface ClientGraphStore {

    /**
     * Get Customer Id
     */
    fun getCustomerId(identity: Identity): Either<StoreError, String>

    /**
     * Get Customer Profile
     */
    fun getCustomer(identity: Identity): Either<StoreError, Customer>

    /**
     * Create Customer Profile
     */
    fun addCustomer(identity: Identity, customer: Customer, referredBy: String? = null): Either<StoreError, Unit>

    /**
     * Update Customer Profile
     */
    fun updateCustomer(identity: Identity, nickname: String?, contactEmail: String?): Either<StoreError, Unit>

    /**
     * Remove Customer for testing
     */
    fun removeCustomer(identity: Identity): Either<StoreError, Unit>

    /**
     * Get Products for a given Customer
     */
    fun getProducts(identity: Identity): Either<StoreError, Map<String, Product>>

    /**
     * Get Product to perform OCS Topup
     */
    fun getProduct(identity: Identity, sku: String): Either<StoreError, Product>


    /**
     * Get Regions (with details) associated with the Customer
     */
    fun getAllRegionDetails(identity: Identity): Either<StoreError, Collection<RegionDetails>>

    /**
     * Get a Region (with details) associated with the Customer
     */
    fun getRegionDetails(identity: Identity, regionCode: String): Either<StoreError, RegionDetails>

    /**
     * Get subscriptions for Customer
     */
    fun getSubscriptions(identity: Identity, regionCode: String? = null): Either<StoreError, Collection<Subscription>>

    /**
     * Get SIM Profiles for Customer
     */
    fun getSimProfiles(identity: Identity, regionCode: String? = null): Either<StoreError, Collection<SimProfile>>

    /**
     * Provision new SIM Profile for Customer
     */
    fun provisionSimProfile(identity: Identity, regionCode: String, profileType: String?): Either<StoreError, SimProfile>

    /**
     * Update SIM Profile for Customer
     */
    fun updateSimProfile(identity: Identity, regionCode: String, iccId: String, alias: String): Either<StoreError, SimProfile>

    /**
     * Provision new SIM Profile for Customer
     */
    fun sendEmailWithActivationQrCode(identity: Identity, regionCode: String, iccId: String): Either<StoreError, SimProfile>

    /**
     * Get balance for Client
     */
    fun getBundles(identity: Identity): Either<StoreError, Collection<Bundle>>

    /**
     * Set balance after OCS Topup or Consumption
     */
    fun updateBundle(bundle: Bundle): Either<StoreError, Unit>

    /**
     * Set balance after OCS Topup or Consumption
     */
    suspend fun consume(msisdn: String, usedBytes: Long, requestedBytes: Long, callback: (Either<StoreError, ConsumptionResult>) -> Unit)

    /**
     * Get all PurchaseRecords
     */
    fun getPurchaseRecords(identity: Identity): Either<StoreError, Collection<PurchaseRecord>>

    /**
     * Add PurchaseRecord after Purchase operation
     */
    fun addPurchaseRecord(customerId: String, purchase: PurchaseRecord): Either<StoreError, String>

    /**
     * Get list of users this user has referred to
     */
    fun getReferrals(identity: Identity): Either<StoreError, Collection<String>>

    /**
     * Get user who has referred this user.
     */
    fun getReferredBy(identity: Identity): Either<StoreError, String?>

    /**
     * Temporary method to perform purchase as atomic transaction
     */
    fun purchaseProduct(identity: Identity, sku: String, sourceId: String?, saveCard: Boolean): Either<PaymentError, ProductInfo>

    /**
     * Generate new eKYC scanId for the customer.
     */
    fun createNewJumioKycScanId(identity: Identity, regionCode: String): Either<StoreError, ScanInformation>

    /**
     * Get the country code for the scan.
     */
    fun getCountryCodeForScan(scanId: String): Either<StoreError, String>

    /**
     * Get information about an eKYC scan for the customer.
     */
    fun getScanInformation(identity: Identity, scanId: String): Either<StoreError, ScanInformation>

    /**
     * Get Customer Data from Singapore MyInfo Data using authorisationCode, and store and return it
     */
    fun getCustomerMyInfoData(identity: Identity, authorisationCode: String): Either<StoreError, String>

    /**
     * Validate and store NRIC/FIN ID
     */
    fun checkNricFinIdUsingDave(identity: Identity, nricFinId: String): Either<StoreError, Unit>

    /**
     * Save address and Phone number
     */
    fun saveAddressAndPhoneNumber(identity: Identity, address: String, phoneNumber: String) : Either<StoreError, Unit>
}

data class ConsumptionResult(val msisdnAnalyticsId: String, val granted: Long, val balance: Long)

interface AdminGraphStore {

    fun getMsisdnToBundleMap(): Map<Subscription, Bundle>
    fun getAllBundles(): Collection<Bundle>
    fun getCustomerToBundleIdMap(): Map<Customer, Bundle>
    fun getCustomerToMsisdnMap(): Map<Customer, Subscription>
    fun getCustomerForMsisdn(msisdn: String): Either<StoreError, Customer>

    /**
     * Link Customer to MSISDN
     */
    @Deprecated(message = "Assigning MSISDN to Customer via Admin API will be removed in future.")
    fun addSubscription(identity: Identity, msisdn: String): Either<StoreError, Unit>

    // simple create
    fun createProductClass(productClass: ProductClass): Either<StoreError, Unit>

    fun createProduct(product: Product): Either<StoreError, Unit>
    fun createSegment(segment: Segment): Either<StoreError, Unit>
    fun createOffer(offer: Offer): Either<StoreError, Unit>

    // simple update
    // updating an Offer and Product is not allowed
    fun updateSegment(segment: Segment): Either<StoreError, Unit>

    fun getCustomerCount(): Long
    fun getReferredCustomerCount(): Long
    fun getPaidCustomerCount(): Long

    /* For managing plans and subscription to plans. */

    /**
     * Get details for a specific plan.
     * @param planId - The name/id of the plan
     * @return Plan details if found
     */
    fun getPlan(planId: String): Either<StoreError, Plan>

    /**
     * Get all plans that a customer subscribes to.
     * @param identity - The identity of the customer
     * @return List with plan details if found
     */
    fun getPlans(identity: Identity): Either<StoreError, List<Plan>>

    /**
     * Create a new plan.
     * @param plan - Plan details
     * @return Unit value if created successfully
     */
    fun createPlan(plan: Plan): Either<StoreError, Plan>

    /**
     * Remove a plan.
     * @param planId - The name/id of the plan
     * @return Unit value if removed successfully
     */
    fun deletePlan(planId: String): Either<StoreError, Plan>

    /**
     * Set up a customer with a subscription to a specific plan.
     * @param identity - The identity of the customer
     * @param planId - The name/id of the plan
     * @param trialEnd - Epoch timestamp for when the trial period ends
     * @return Unit value if the subscription was created successfully
     */
    fun subscribeToPlan(identity: Identity, planId: String, trialEnd: Long = 0): Either<StoreError, Plan>

    /**
     * Remove the subscription to a plan for a specific subscrber.
     * @param identity - The identity of the customer
     * @param planId - The name/id of the plan
     * @param atIntervalEnd - Remove at end of curren subscription period
     * @return Unit value if the subscription was removed successfully
     */
    fun unsubscribeFromPlan(identity: Identity, planId: String, atIntervalEnd: Boolean = false): Either<StoreError, Plan>

    /**
     * Adds a purchase record to customer on start of or renewal
     * of a subscription.
     * @param invoiceId - The reference to the invoice that has been paid
     * @param customerId - The customer that got charged
     * @param sku - The product/plan bought
     * @param amount - Cost of the product/plan
     * @param currency - Currency used
     */
    fun purchasedSubscription(invoiceId: String, customerId: String, sku: String, amount: Long, currency: String): Either<StoreError, Plan>

    // atomic import of Offer + Product + Segment
    fun atomicCreateOffer(
            offer: Offer,
            segments: Collection<Segment> = emptyList(),
            products: Collection<Product> = emptyList()): Either<StoreError, Unit>

    fun atomicCreateSegments(createSegments: Collection<Segment>): Either<StoreError, Unit>

    fun atomicUpdateSegments(updateSegments: Collection<Segment>): Either<StoreError, Unit>
    fun atomicAddToSegments(addToSegments: Collection<Segment>): Either<StoreError, Unit>
    fun atomicRemoveFromSegments(removeFromSegments: Collection<Segment>): Either<StoreError, Unit>
    fun atomicChangeSegments(changeSegments: Collection<ChangeSegment>): Either<StoreError, Unit>

    // Method to perform a full refund of a purchase
    fun refundPurchase(identity: Identity, purchaseRecordId: String, reason: String): Either<PaymentError, ProductInfo>

    // update the scan information with scan result
    fun updateScanInformation(scanInformation: ScanInformation, vendorData: MultivaluedMap<String, String>): Either<StoreError, Unit>

    // Retrieve all scan information for the customer
    fun getAllScanInformation(identity: Identity): Either<StoreError, Collection<ScanInformation>>

    fun createRegion(region: Region): Either<StoreError, Unit>

    // simple getAll
    // fun getOffers(): Collection<Offer>
    // fun getSegments(): Collection<Segment>
    // fun getSubscribers(): Collection<Subscriber>
    // fun getProducts(): Collection<Product>
    // fun getProductClasses(): Collection<ProductClass>

    // simple get by id
    // fun getOffer(id: String): Offer?
    // fun getSegment(id: String): Segment?
    // fun getProductClass(id: String): ProductClass?
}

interface ScanInformationStore {
    // Function to upsert scan information data from the 3rd party eKYC scan
    fun upsertVendorScanInformation(customerId: String, countryCode: String, vendorData: MultivaluedMap<String, String>): Either<StoreError, Unit>
    fun getExtendedStatusInformation(vendorData: MultivaluedMap<String, String>): Map<String, String>
}