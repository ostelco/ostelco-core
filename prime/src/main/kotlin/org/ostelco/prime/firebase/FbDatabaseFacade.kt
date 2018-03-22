package org.ostelco.prime.firebase

import com.google.common.base.Preconditions.checkNotNull
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.ostelco.prime.logger
import org.ostelco.prime.storage.ProductCatalogItem
import org.ostelco.prime.storage.StorageException
import org.ostelco.prime.storage.entities.PurchaseRequest
import org.ostelco.prime.storage.entities.PurchaseRequestImpl
import org.ostelco.prime.storage.entities.RecordOfPurchaseImpl
import org.ostelco.prime.storage.entities.Subscriber
import org.ostelco.prime.storage.entities.SubscriberImpl
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction
import java.util.function.Consumer

/**
 * Presenting a facade for the many and  varied firebase databases
 * we're using.
 */
class FbDatabaseFacade internal constructor(firebaseDatabase: FirebaseDatabase) {

    private val LOG by logger()

    private val authorativeUserData: DatabaseReference

    private val clientRequests: DatabaseReference

    private val clientVisibleSubscriberRecords: DatabaseReference

    private val recordsOfPurchase: DatabaseReference

    private val quickBuyProducts: DatabaseReference

    private val products: DatabaseReference

    // Ignoring this, not a fatal failure.
    val allSubscribers: Collection<Subscriber>
        get() {
            val q = authorativeUserData.orderByKey()
            val subscribers = LinkedHashSet<Subscriber>()
            val cdl = CountDownLatch(1)
            val collectingVisitor = newListenerThatWillCollectAllSubscribers(subscribers, cdl)

            q.addListenerForSingleValueEvent(collectingVisitor)

            try {
                cdl.await(SECONDS_TO_WAIT_FOR_FIREBASE.toLong(), TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
            }

            if (cdl.count > 0) {
                LOG.error("Failed to get all subscribers")
            }
            return subscribers
        }

    init {
        checkNotNull(firebaseDatabase)

        this.authorativeUserData = firebaseDatabase.getReference("authorative-user-storage")
        this.clientRequests = firebaseDatabase.getReference("client-requests")
        this.clientVisibleSubscriberRecords = firebaseDatabase.getReference("profiles")
        this.recordsOfPurchase = firebaseDatabase.getReference("records-of-purchase")

        // Used to listen in on new products from the Firebase product catalog.
        this.quickBuyProducts = firebaseDatabase.getReference("quick-buy-products")
        this.products = firebaseDatabase.getReference("products")
    }

    private fun newCatalogDataChangedEventListener(
            consumer: Consumer<ProductCatalogItem>): ValueEventListener {
        checkNotNull(consumer)
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                LOG.info("onDataChange")
                // FIXME method below adds change listener to products. The serialization logic is broken.
                // interpretDataSnapshotAsProductCatalogItem(snapshot, consumer);
            }

            override fun onCancelled(error: DatabaseError) {
                // Intentionally left blank.
            }
        }
    }

    private fun interpretDataSnapshotAsProductCatalogItem(
            snapshot: DataSnapshot,
            consumer: Consumer<ProductCatalogItem>) {
        checkNotNull(consumer)
        if (snapshotIsInvalid(snapshot)) {
            return
        }

        try {
            val item = snapshot.getValue(ProductCatalogItem::class.java)
            if (item.sku != null) {
                consumer.accept(item)
            }
            LOG.info("Just read a product catalog item: " + item)
        } catch (e: Exception) {
            LOG.error("Couldn't transform req into ProductCatalogItem", e)
        }

    }


    private fun addOrUpdateProduct(
            snapshot: DataSnapshot,
            consumer: Consumer<ProductCatalogItem>) {
        checkNotNull(consumer)
        if (snapshotIsInvalid(snapshot)) {
            return
        }

        try {
            val item = snapshot.getValue(ProductCatalogItem::class.java)
            if (item.sku != null) {
                consumer.accept(item)
            }
            LOG.info("Just read a product catalog item: {}", item)
        } catch (e: Exception) {
            LOG.error("Couldn't transform req into PurchaseRequestImpl", e)
        }

    }

    fun addProductCatalogItemListener(consumer: Consumer<ProductCatalogItem>) {
        addProductCatalogItemChildListener(consumer)
        addProductCatalogValueListener(consumer)
    }

    fun addPurchaseRequestListener(
            consumer: BiFunction<String, PurchaseRequestImpl, Unit>) {
        addPurchaseEventListener(listenerForPurchaseRequests(consumer))
    }


    fun addProductCatalogItemChildListener(consumer: Consumer<ProductCatalogItem>) {
        checkNotNull(consumer)
        val productCatalogListener = newProductDefChangedListener(Consumer { snapshot -> addOrUpdateProduct(snapshot, consumer) })
        addProductCatalogListener(productCatalogListener)
    }

    fun addProductCatalogListener(consumer: Consumer<DataSnapshot>) {
        checkNotNull(consumer)
        val productCatalogListener = newProductDefChangedListener(consumer)
        addProductCatalogListener(productCatalogListener)
    }


    fun addProductCatalogListener(productCatalogListener: ChildEventListener) {
        checkNotNull(productCatalogListener)
        this.quickBuyProducts.addChildEventListener(productCatalogListener)
        this.products.addChildEventListener(productCatalogListener)
    }

    fun addProductCatalogValueListener(consumer: Consumer<ProductCatalogItem>) {
        checkNotNull(consumer)
        val productCatalogValueEventListener = newCatalogDataChangedEventListener(consumer)
        addProductCatalogValueListener(productCatalogValueEventListener)
    }


    fun addProductCatalogValueListener(
            productCatalogValueEventListener: ValueEventListener) {
        checkNotNull(productCatalogValueEventListener)
        this.quickBuyProducts.addValueEventListener(productCatalogValueEventListener)
        this.products.addValueEventListener(productCatalogValueEventListener)
    }

    fun addPurchaseEventListener(cel: ChildEventListener) {
        checkNotNull(cel)
        this.clientRequests.addChildEventListener(cel)
    }


    fun addRecordOfPurchaseByMsisdn(
            msisdn: String,
            sku: String,
            millisSinceEpoch: Long): String {
        checkNotNull(msisdn)

        val purchase = RecordOfPurchaseImpl(msisdn, sku, millisSinceEpoch)

        // XXX This is iffy, why not send the purchase object
        //     directly to the facade.  Seems bogus, probably is.
        val asMap = purchase.asMap()

        return pushRecordOfPurchaseByMsisdn(asMap)
    }

    fun pushRecordOfPurchaseByMsisdn(asMap: Map<String, Any>): String {
        checkNotNull(asMap)
        val dbref = recordsOfPurchase.push()

        dbref.updateChildren(asMap)
        return dbref.key
    }

    @Throws(StorageException::class)
    fun updateClientVisibleUsageString(
            msisdn: String,
            gbLeft: String) {
        checkNotNull(msisdn)
        checkNotNull(gbLeft)
        val key = getKeyFromPhoneNumber(clientVisibleSubscriberRecords, msisdn)
        if (key == null) {
            LOG.error("Could not find entry for phoneNumber = "
                    + msisdn
                    + " Not updating user visible storage")
            return
        }

        val displayRep = HashMap<String, Any>()
        displayRep[PHONE_NUMBER] = msisdn

        displayRep["usage"] = gbLeft

        clientVisibleSubscriberRecords.child(key).updateChildren(displayRep)
    }


    @Throws(StorageException::class)
    private fun getKeyFromPhoneNumber(
            dbref: DatabaseReference,
            msisdn: String): String? {
        return getKeyFromLookupKey(dbref, msisdn, PHONE_NUMBER)
    }

    @Throws(StorageException::class)
    private fun getKeyFromMsisdn(
            dbref: DatabaseReference,
            msisdn: String): String? {
        return getKeyFromLookupKey(dbref, msisdn, MSISDN)
    }

    @Throws(StorageException::class)
    private fun removeByMsisdn(
            dbref: DatabaseReference,
            msisdn: String) {
        checkNotNull(msisdn)
        checkNotNull(dbref)
        val key = getKeyFromMsisdn(dbref, msisdn)
        if (key != null) {
            removeChild(dbref, key)
        }
    }

    @Throws(StorageException::class)
    fun removeByMsisdn(msisdn: String) {
        removeByMsisdn(clientVisibleSubscriberRecords, msisdn)
    }

    @Throws(StorageException::class)
    fun removeSubscriberByMsisdn(msisdn: String) {
        removeByMsisdn(authorativeUserData, msisdn)
    }


    @Throws(StorageException::class)
    private fun getKeyFromLookupKey(
            dbref: DatabaseReference,
            msisdn: String,
            lookupKey: String): String? {
        val cdl = CountDownLatch(1)
        val result = HashSet<String>()
        dbref.orderByChild(lookupKey).equalTo(msisdn).limitToFirst(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // XXX This is unclean, fix!
                handleDataChange(snapshot, cdl, result, msisdn)
            }

            override fun onCancelled(error: DatabaseError) {
                // Empty on purpose.
            }
        })
        try {
            return if (!cdl.await(SECONDS_TO_WAIT_FOR_FIREBASE.toLong(), TimeUnit.SECONDS)) {
                throw StorageException("Query timed out")
            } else if (result.isEmpty()) {
                null
            } else {
                result.iterator().next()
            }
        } catch (e: InterruptedException) {
            throw StorageException(e)
        }

    }

    fun injectPurchaseRequest(pr: PurchaseRequest): String {
        val cr = pr as PurchaseRequestImpl
        val dbref = clientRequests.push()
        val crAsMap = cr.asMap()
        dbref.setValue(crAsMap)
        return dbref.key
    }

    fun removeRecordOfPurchaseById(id: String) {
        removeChild(recordsOfPurchase, id)
    }

    private fun removeChild(db: DatabaseReference, childId: String) {
        // XXX Removes whole tree, not just the subtree for id.
        //     how do I fix this?
        checkNotNull(db)
        checkNotNull(childId)
        db.child(childId).ref.removeValue()
    }

    fun removePurchaseRequestById(id: String) {
        checkNotNull(id)
        removeChild(clientRequests, id)
    }

    @Throws(StorageException::class)
    fun getSubscriberFromMsisdn(msisdn: String): Subscriber? {
        val cdl = CountDownLatch(1)
        val result = HashSet<Subscriber>()

        val q = authorativeUserData.orderByChild(MSISDN).equalTo(msisdn).limitToFirst(1)

        val listenerThatWillReadSubcriberData = newListenerThatWillReadSubcriberData(cdl, result)

        q.addListenerForSingleValueEvent(
                listenerThatWillReadSubcriberData)

        return waitForSubscriberData(msisdn, cdl, result)
    }

    private fun logSubscriberDataProcessing(
            msisdn: String,
            userData: String,
            result: String): String {
        val msg = ("authorativeuserdata = '" + userData
                + "', msisdn = '" + msisdn
                + "' => " + result)
        LOG.info(msg)
        return msg
    }

    @Throws(StorageException::class)
    private fun waitForSubscriberData(
            msisdn: String,
            cdl: CountDownLatch,
            result: Set<Subscriber>): Subscriber? {
        val userDataString = authorativeUserData.toString()
        try {
            return if (!cdl.await(SECONDS_TO_WAIT_FOR_FIREBASE.toLong(), TimeUnit.SECONDS)) {
                val msg = logSubscriberDataProcessing(
                        msisdn, userDataString, "timeout")
                throw StorageException(msg)
            } else if (result.isEmpty()) {
                logSubscriberDataProcessing(msisdn, userDataString, "null")
                null
            } else {
                val r = result.iterator().next()
                logSubscriberDataProcessing(msisdn, userDataString, r.toString())
                r
            }
        } catch (e: InterruptedException) {
            val msg = logSubscriberDataProcessing(msisdn, userDataString, "interrupted")
            throw StorageException(msg, e)
        }

    }

    private fun newListenerThatWillReadSubcriberData(
            cdl: CountDownLatch,
            result: MutableSet<Subscriber>): ValueEventListener {
        return object : AbstractValueEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChildren()) {
                    cdl.countDown()
                } else {
                    for (snap in snapshot.children) {
                        val sub = snap.getValue(SubscriberImpl::class.java)
                        val key = snap.key
                        sub.fbKey = key
                        result.add(sub)
                        cdl.countDown()
                    }
                }
            }
        }
    }

    fun updateAuthorativeUserData(sub: SubscriberImpl) {
        checkNotNull(sub)
        val dbref = authorativeUserData.child(sub.fbKey!!)
        dbref.updateChildren(sub.asMap())
    }

    fun insertNewSubscriber(sub: SubscriberImpl): String {
        checkNotNull(sub)
        val dbref = authorativeUserData.push()
        sub.fbKey = dbref.key
        dbref.updateChildren(sub.asMap())
        return dbref.key
    }

    fun newProductDefChangedListener(
            snapshotConsumer: Consumer<DataSnapshot>): ChildEventListener {
        return object : AbstractChildEventListener() {
            override fun onChildAdded(snapshot: DataSnapshot,
                                      previousChildName: String) {
                snapshotConsumer.accept(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot,
                                        previousChildName: String) {
                snapshotConsumer.accept(snapshot)
            }
        }
    }

    companion object {

        private val LOG by logger()

        private const val PHONE_NUMBER = "phoneNumber"

        private const val MSISDN = "msisdn"

        private const val SECONDS_TO_WAIT_FOR_FIREBASE = 10

        private fun listenerForPurchaseRequests(
                consumer: BiFunction<String, PurchaseRequestImpl, Unit>): AbstractChildEventListener {
            checkNotNull(consumer)
            return object : AbstractChildEventListener() {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String) {
                    if (snapshotIsInvalid(snapshot)) {
                        return
                    }
                    try {
                        val req = snapshot.getValue(PurchaseRequestImpl::class.java)
                        consumer.apply(snapshot.key, req)
                    } catch (e: Exception) {
                        LOG.error("Couldn't dispatch purchase request to consumer", e)
                    }

                }
            }
        }


        private fun snapshotIsInvalid(snapshot: DataSnapshot?): Boolean {
            if (snapshot == null) {
                LOG.error("dataSnapshot can't be null")
                return true
            }

            if (!snapshot.exists()) {
                LOG.error("dataSnapshot must exist")
                return true
            }
            return false
        }

        // XXX Should this be removed? Doesn't look nice.
        private fun handleDataChange(
                snapshot: DataSnapshot,
                cdl: CountDownLatch,
                result: MutableSet<String>,
                msisdn: String) {

            if (!snapshot.hasChildren()) {
                cdl.countDown()
                return
            }

            try {
                for (snap in snapshot.children) {
                    val key = snap.key
                    result.add(key)
                    cdl.countDown()
                }
            } catch (e: Exception) {
                LOG.error("Something happened while looking for key = " + msisdn, e)
            }

        }

        private fun newListenerThatWillCollectAllSubscribers(
                subscribers: MutableSet<Subscriber>,
                cdl: CountDownLatch): ValueEventListener {
            checkNotNull<Set<Subscriber>>(subscribers)
            checkNotNull(cdl)
            return object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.hasChildren()) {
                        cdl.countDown()
                        return
                    }
                    for (child in snapshot.children) {
                        val subscriber = child.getValue(SubscriberImpl::class.java)
                        subscriber.fbKey = child.key
                        subscribers.add(subscriber)
                    }
                    cdl.countDown()
                }

                override fun onCancelled(error: DatabaseError) {
                    LOG.error(error.message, error.toException())
                }
            }
        }
    }
}
