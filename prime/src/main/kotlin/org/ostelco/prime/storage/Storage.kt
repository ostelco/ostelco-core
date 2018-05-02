package org.ostelco.prime.storage


import org.ostelco.prime.storage.entities.PurchaseRequest
import org.ostelco.prime.storage.entities.RecordOfPurchase
import org.ostelco.prime.storage.entities.Subscriber

/**
 * Interface that abstracts the interactions that
 * are necessary to get/update customer data and to both
 * with respect to (slow) accounting, and (fast) provisioning.
 * Typically this interface will represent a facade towards
 * multiple specialized storage solutions.
 */
interface Storage : ProductDescriptionCache {

    val allSubscribers: Collection<Subscriber>
    // XXX Shouldn't extend anything I think.

    fun injectPurchaseRequest(pr: PurchaseRequest): String

    @Throws(StorageException::class)
    fun updateDisplayDatastructure(msisdn: String)

    @Throws(StorageException::class)
    fun removeDisplayDatastructure(msisdn: String)

    @Throws(StorageException::class)
    fun setRemainingByMsisdn(msisdn: String?, noOfBytes: Long)

    @Throws(StorageException::class)
    fun getSubscriberFromMsisdn(msisdn: String): Subscriber?

    @Throws(StorageException::class)
    fun insertNewSubscriber(msisdn: String)

    @Throws(StorageException::class)
    fun removeSubscriberByMsisdn(msisdn: String)

    fun addPurchaseRequestHandler(handler: PurchaseRequestHandler)

    @Throws(StorageException::class)
    fun addRecordOfPurchase(purchase: RecordOfPurchase): String

    fun removePurchaseRequestById(id: String)

    fun removeRecordOfPurchaseById(id: String)
}
