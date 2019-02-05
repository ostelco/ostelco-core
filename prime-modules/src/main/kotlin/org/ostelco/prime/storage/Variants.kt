package org.ostelco.prime.storage

import arrow.core.Either
import org.ostelco.prime.model.*
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import com.google.cloud.datastore.Blob
import javax.ws.rs.core.MultivaluedMap

interface ClientDocumentStore {

    /**
     * Get token used for sending notification to user application
     */
    fun getNotificationTokens(msisdn : String): Collection<ApplicationToken>

    /**
     * Add token used for sending notification to user application
     */
    fun addNotificationToken(msisdn: String, token: ApplicationToken) : Boolean

    /**
     * Get token used for sending notification to user application
     */
    fun getNotificationToken(msisdn: String, applicationID: String): ApplicationToken?

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
     * Get Subscriber Profile
     */
    fun getSubscriber(subscriberId: String): Either<StoreError, Subscriber>

    /**
     * Create Subscriber Profile
     */
    fun addSubscriber(subscriber: Subscriber, referredBy: String? = null): Either<StoreError, Unit>

    /**
     * Update Subscriber Profile
     */
    fun updateSubscriber(subscriber: Subscriber): Either<StoreError, Unit>

    /**
     * Remove Subscriber for testing
     */
    fun removeSubscriber(subscriberId: String): Either<StoreError, Unit>

    /**
     * Link Subscriber to MSISDN
     */
    fun addSubscription(subscriberId: String, msisdn: String): Either<StoreError, Unit>

    /**
     * Get Products for a given subscriber
     */
    fun getProducts(subscriberId: String): Either<StoreError, Map<String, Product>>

    /**
     * Get Product to perform OCS Topup
     */
    fun getProduct(subscriberId: String, sku: String): Either<StoreError, Product>

    /**
     * Get subscriptions for Client
     */
    fun getSubscriptions(subscriberId: String): Either<StoreError, Collection<Subscription>>

    /**
     * Get balance for Client
     */
    fun getBundles(subscriberId: String): Either<StoreError, Collection<Bundle>>

    /**
     * Set balance after OCS Topup or Consumption
     */
    fun updateBundle(bundle: Bundle): Either<StoreError, Unit>

    /**
     * Set balance after OCS Topup or Consumption
     */
    fun consume(msisdn: String, usedBytes: Long, requestedBytes: Long): Either<StoreError, Pair<Long, Long>>

    /**
     * Get msisdn for the given subscription-id
     */
    fun getMsisdn(subscriptionId: String): Either<StoreError, String>

    /**
     * Get all PurchaseRecords
     */
    fun getPurchaseRecords(subscriberId: String): Either<StoreError, Collection<PurchaseRecord>>

    /**
     * Add PurchaseRecord after Purchase operation
     */
    fun addPurchaseRecord(subscriberId: String, purchase: PurchaseRecord): Either<StoreError, String>

    /**
     * Get list of users this user has referred to
     */
    fun getReferrals(subscriberId: String): Either<StoreError, Collection<String>>

    /**
     * Get user who has referred this user.
     */
    fun getReferredBy(subscriberId: String): Either<StoreError, String?>

    /**
     * Temporary method to perform purchase as atomic transaction
     */
    fun purchaseProduct(subscriberId: String, sku: String, sourceId: String?, saveCard: Boolean): Either<PaymentError, ProductInfo>

    /**
     * Generate new eKYC scanId for the subscriber.
     */
    fun newEKYCScanId(subscriberId: String, countryCode: String): Either<StoreError, ScanInformation>

    /**
     * Get the country code for the scan.
     */
    fun getCountryCodeForScan(scanId: String): Either<StoreError, String>

    /**
     * Get information about an eKYC scan for the subscriber.
     */
    fun getScanInformation(subscriberId: String, scanId: String): Either<StoreError, ScanInformation>

    /**
     * Get state of the subscriber.
     */
    fun getSubscriberState(subscriberId: String): Either<StoreError, SubscriberState>
}

interface AdminGraphStore {

    fun getMsisdnToBundleMap(): Map<Subscription, Bundle>
    fun getAllBundles(): Collection<Bundle>
    fun getSubscriberToBundleIdMap(): Map<Subscriber, Bundle>
    fun getSubscriberToMsisdnMap(): Map<Subscriber, Subscription>
    fun getSubscriberForMsisdn(msisdn: String): Either<StoreError, Subscriber>

    // simple create
    fun createProductClass(productClass: ProductClass): Either<StoreError, Unit>
    fun createProduct(product: Product): Either<StoreError, Unit>
    fun createSegment(segment: Segment): Either<StoreError, Unit>
    fun createOffer(offer: Offer): Either<StoreError, Unit>

    // simple update
    // updating an Offer and Product is not allowed
    fun updateSegment(segment: Segment): Either<StoreError, Unit>

    fun getSubscriberCount(): Long
    fun getReferredSubscriberCount(): Long
    fun getPaidSubscriberCount(): Long

    /* For managing plans and subscription to plans. */

    /**
     * Get details for a specific plan.
     * @param planId - The name/id of the plan
     * @return Plan details if found
     */
    fun getPlan(planId: String): Either<StoreError, Plan>

    /**
     * Get all plans that a subscriber subscribes to.
     * @param subscriberId - The subscriber
     * @return List with plan details if found
     */
    fun getPlans(subscriberId: String): Either<StoreError, List<Plan>>

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
     * Set up a subscriber with a subscription to a specific plan.
     * @param subscriberId - The id of the subscriber
     * @param planId - The name/id of the plan
     * @param trialEnd - Epoch timestamp for when the trial period ends
     * @return Unit value if the subscription was created successfully
     */
    fun subscribeToPlan(subscriberId: String, planId: String, trialEnd: Long = 0): Either<StoreError, Plan>

    /**
     * Remove the subscription to a plan for a specific subscrber.
     * @param subscriberId - The id of the subscriber
     * @param planId - The name/id of the plan
     * @param atIntervalEnd - Remove at end of curren subscription period
     * @return Unit value if the subscription was removed successfully
     */
    fun unsubscribeFromPlan(subscriberId: String, planId: String, atIntervalEnd: Boolean = false): Either<StoreError, Plan>

    /**
     * Adds a purchase record to subscriber on start of or renewal
     * of a subscription.
     * @param invoiceId - The reference to the invoice that has been paid
     * @param subscriberId - The subscriber that got charged
     * @param sku - The product/plan bought
     * @param amount - Cost of the product/plan
     * @param currency - Currency used
     */
    fun subscriptionPurchaseReport(invoiceId: String, subscriberId: String, sku: String, amount: Long, currency: String): Either<StoreError, Plan>

    // atomic import of Offer + Product + Segment
    fun atomicCreateOffer(
            offer: Offer,
            segments: Collection<Segment> = emptyList(),
            products: Collection<Product> = emptyList()) : Either<StoreError, Unit>

    fun atomicCreateSegments(createSegments: Collection<Segment>): Either<StoreError, Unit>

    fun atomicUpdateSegments(updateSegments: Collection<Segment>): Either<StoreError, Unit>
    fun atomicAddToSegments(addToSegments: Collection<Segment>): Either<StoreError, Unit>
    fun atomicRemoveFromSegments(removeFromSegments: Collection<Segment>): Either<StoreError, Unit>
    fun atomicChangeSegments(changeSegments: Collection<ChangeSegment>): Either<StoreError, Unit>

    // Method to perform a full refund of a purchase
    fun refundPurchase(subscriberId: String, purchaseRecordId: String, reason: String): Either<PaymentError, ProductInfo>

    // update the scan information with scan result
    fun updateScanInformation(scanInformation: ScanInformation, vendorData: MultivaluedMap<String, String>): Either<StoreError, Unit>

    // Retrieve all scan information for the subscriber
    fun getAllScanInformation(subscriberId: String): Either<StoreError, Collection<ScanInformation>>

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
    fun upsertVendorScanInformation(subscriberId: String, countryCode: String, vendorData: MultivaluedMap<String, String>): Either<StoreError, Unit>
}