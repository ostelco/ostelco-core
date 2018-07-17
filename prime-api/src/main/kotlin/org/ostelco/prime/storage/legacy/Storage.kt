package org.ostelco.prime.storage.legacy

import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber


/**
 * Interface that abstracts the interactions that
 * are necessary to get/update customer data and to both
 * with respect to (slow) accounting, and (fast) provisioning.
 * Typically this interface will represent a facade towards
 * multiple specialized storage solutions.
 */
interface Storage {

    val balances: Map<String, Long>

    /**
     * Get Subscriber Profile
     */
    @Throws(StorageException::class)
    fun getSubscriber(id: String): Subscriber?

    /**
     * Create Subscriber Profile
     */
    @Throws(StorageException::class)
    fun addSubscriber(subscriber: Subscriber): Boolean

    /**
     * Update Subscriber Profile
     */
    @Throws(StorageException::class)
    fun updateSubscriber(subscriber: Subscriber): Boolean

    /**
     * Remove Subscriber for testing
     */
    @Throws(StorageException::class)
    fun removeSubscriber(id: String): Boolean

    /**
     * Link Subscriber to MSISDN
     */
    @Throws(StorageException::class)
    fun addSubscription(id: String, msisdn: String): Boolean

    /**
     * Get Products for a given subscriber
     */
    @Throws(StorageException::class)
    fun getProducts(subscriberId: String): Map<String, Product>

    /**
     * Get Product to perform OCS Topup
     */
    @Throws(StorageException::class)
    fun getProduct(subscriberId: String?, sku: String): Product?

    /**
     * Get balance for Client
     */
    @Throws(StorageException::class)
    fun getBalance(id: String): Long?

    /**
     * Set balance after OCS Topup or Consumption
     */
    @Throws(StorageException::class)
    fun setBalance(msisdn: String, noOfBytes: Long): Boolean

    /**
     * Get msisdn for the given subscription-id
     */
    @Throws(StorageException::class)
    fun getMsisdn(subscriptionId: String): String?

    /**
     * Get all PurchaseRecords
     */
    @Throws(StorageException::class)
    fun getPurchaseRecords(id: String): Collection<PurchaseRecord>

    /**
     * Add PurchaseRecord after Purchase operation
     */
    @Throws(StorageException::class)
    fun addPurchaseRecord(id: String, purchase: PurchaseRecord): String?

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

    /**
     *
     */
    fun getPaymentId(id: String): String?

    /**
     *
     */
    fun deletePaymentId(id: String): Boolean

    /**
     *
     */
    fun createPaymentId(id: String, paymentId: String): Boolean

    /**
     *
     */
    fun getCustomerId(id: String): String?
}
