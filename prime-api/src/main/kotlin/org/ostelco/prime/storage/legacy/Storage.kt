package org.ostelco.prime.storage.legacy

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
     * Create Subscriber Profile
     */
    @Throws(StorageException::class)
    fun addSubscriber(id: String, subscriber: Subscriber): Boolean

    /**
     * Get Subscriber Profile
     */
    @Throws(StorageException::class)
    fun getSubscriber(id: String): Subscriber?

    /**
     * Update Subscriber Profile
     */
    @Throws(StorageException::class)
    fun updateSubscriber(id: String, subscriber: Subscriber): Boolean

    /**
     * Remove Subscriber for testing
     */
    @Throws(StorageException::class)
    fun removeSubscriber(id: String)


    /**
     * Link Subscriber to MSISDN
     */
    @Throws(StorageException::class)
    fun addSubscription(id: String, msisdn: String)

    /**
     * Get Product to perform OCS Topup
     */
    @Throws(StorageException::class)
    fun getProduct(sku: String): Product?

    /**
     * Get Product to perform OCS Topup
     */
    @Throws(StorageException::class)
    fun getProducts(): Map<String, Product>

    /**
     * Get balance for Client
     */
    @Throws(StorageException::class)
    fun getBalance(email: String): Long?

    /**
     * Set balance after OCS Topup or Consumption
     */
    @Throws(StorageException::class)
    fun setBalance(msisdn: String, noOfBytes: Long): Boolean

    /**
     * Add PurchaseRecord after Purchase operation
     */
    @Throws(StorageException::class)
    fun addPurchaseRecord(purchase: PurchaseRecord): String?
}
