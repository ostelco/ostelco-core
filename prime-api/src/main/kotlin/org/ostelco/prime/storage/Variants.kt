package org.ostelco.prime.storage

import arrow.core.Either
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Offer
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.ProductClass
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Segment
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.paymentprocessor.core.PaymentError
import org.ostelco.prime.paymentprocessor.core.ProductInfo

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
    fun getBundles(subscriberId: String): Either<StoreError, Collection<Bundle>?>

    /**
     * Set balance after OCS Topup or Consumption
     */
    fun updateBundle(bundle: Bundle): Either<StoreError, Unit>

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
}

interface AdminGraphStore {

    fun getMsisdnToBundleMap(): Map<Subscription, Bundle>
    fun getAllBundles(): Collection<Bundle>
    fun getSubscriberToBundleIdMap(): Map<Subscriber, Bundle>
    fun getSubscriberToMsisdnMap(): Map<Subscriber, Subscription>

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
