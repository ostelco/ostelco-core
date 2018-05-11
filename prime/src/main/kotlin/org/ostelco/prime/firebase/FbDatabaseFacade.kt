package org.ostelco.prime.firebase

import com.google.common.base.Preconditions.checkNotNull
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.ostelco.prime.firebase.entities.FbPurchaseRequest
import org.ostelco.prime.firebase.entities.FbSubscriber
import org.ostelco.prime.firebase.entities.asMap
import org.ostelco.prime.logger
import org.ostelco.prime.model.ProductCatalogItem
import org.ostelco.prime.model.PurchaseRequest
import org.ostelco.prime.model.RecordOfPurchase
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.storage.StorageException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.BiFunction
import java.util.function.Consumer

/**
 * Presenting a facade for the many and varied firebase databases
 * we're using.
 */
class FbDatabaseFacade internal constructor(firebaseDatabase: FirebaseDatabase) {

    private val LOG by logger()

    private val authorativeUserBalance: DatabaseReference

    private val clientRequests: DatabaseReference

    private val clientVisibleSubscriberRecords: DatabaseReference

    private val recordsOfPurchase: DatabaseReference

    private val quickBuyProducts: DatabaseReference

    private val products: DatabaseReference

    private val notifications: DatabaseReference

    // Ignoring this, not a fatal failure.
    val allSubscribers: Collection<Subscriber>
        get() {
            val subscribers = LinkedHashSet<Subscriber>()
            val cdl = CountDownLatch(1)
            val collectingVisitor = newListenerThatWillCollectAllSubscribers(subscribers, cdl)

            authorativeUserBalance.addListenerForSingleValueEvent(collectingVisitor)

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

        this.authorativeUserBalance = firebaseDatabase.getReference("authorative-user-balance")
        this.clientRequests = firebaseDatabase.getReference("client-requests")
        this.clientVisibleSubscriberRecords = firebaseDatabase.getReference("profiles")
        this.recordsOfPurchase = firebaseDatabase.getReference("records-of-purchase")

        // Used to listen in on new products from the Firebase product catalog.
        this.quickBuyProducts = firebaseDatabase.getReference("quick-buy-products")
        this.products = firebaseDatabase.getReference("products")

        this.notifications = firebaseDatabase.getReference("fcm-requests")
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
            LOG.info("Fetched product catalog item: " + item.sku)
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
            LOG.info("Fetched product catalog item: {}", item.sku)
        } catch (e: Exception) {
            LOG.error("Couldn't transform req into PurchaseRequestImpl", e)
        }
    }

    fun addProductCatalogItemHandler(consumer: Consumer<ProductCatalogItem>) {
        addProductCatalogItemChildHandler(consumer)
        addProductCatalogValueHandler(consumer)
    }

    /**
     * Add a listener for Purchase Request
     */
    fun addPurchaseRequestListener(consumer: BiFunction<String, PurchaseRequest, Unit>) {
        val childEventListener = listenerForPurchaseRequests(consumer)
        checkNotNull(childEventListener)
        this.clientRequests.addChildEventListener(childEventListener)
    }

    fun addProductCatalogItemChildHandler(consumer: Consumer<ProductCatalogItem>) {
        checkNotNull(consumer)
        val productCatalogListener = newProductDefChangedListener(Consumer { snapshot -> addOrUpdateProduct(snapshot, consumer) })
        addProductCatalogListener(productCatalogListener)
    }


    private fun addProductCatalogItemChildListener(consumer: Consumer<ProductCatalogItem>) {
        checkNotNull(consumer)
        val productCatalogListener = newProductDefChangedListener(Consumer { snapshot -> addOrUpdateProduct(snapshot, consumer) })
        addProductCatalogListener(productCatalogListener)
    }

    private fun addProductCatalogListener(productCatalogListener: ChildEventListener) {
        checkNotNull(productCatalogListener)
        this.quickBuyProducts.addChildEventListener(productCatalogListener)
        this.products.addChildEventListener(productCatalogListener)
    }

    private fun addProductCatalogValueHandler(consumer: Consumer<ProductCatalogItem>) {
        checkNotNull(consumer)
        val productCatalogValueEventListener = newCatalogDataChangedEventListener(consumer)
        addProductCatalogValueListener(productCatalogValueEventListener)
    }


    private fun addProductCatalogValueListener(productCatalogValueEventListener: ValueEventListener) {
        checkNotNull(productCatalogValueEventListener)
        this.quickBuyProducts.addValueEventListener(productCatalogValueEventListener)
        this.products.addValueEventListener(productCatalogValueEventListener)
    }


    /**
     * Store a record of purchase to the db
     */
    fun addRecordOfPurchase(purchase: RecordOfPurchase): String {
        checkNotNull(purchase)
        val asMap = purchase.asMap()
        checkNotNull(asMap)
        val dbref = recordsOfPurchase.push()

        dbref.updateChildrenAsync(asMap)
        return dbref.key
    }

    /**
     * Uses Firebase as an API for sending notifications to the subscriber.
     * A cloud function will listen to the key and send push notification to the client
     * with the cureent balance. Used for low balance notifications
     */
    fun addNotification(subscriber: Subscriber) {
        checkNotNull(subscriber)
        notifications.child(stripLeadingPlus(subscriber.msisdn)).setValueAsync(subscriber.asMap())
    }

    @Throws(StorageException::class)
    fun updateClientVisibleUsageString(
            msisdn: String,
            gbLeft: String) {
        checkNotNull(msisdn)
        checkNotNull(gbLeft)

        val displayRep = HashMap<String, Any>()
        displayRep[PHONE_NUMBER] = msisdn

        displayRep["usage"] = gbLeft

        clientVisibleSubscriberRecords.child(stripLeadingPlus(msisdn)).updateChildrenAsync(displayRep)
    }


    @Throws(StorageException::class)
    private fun removeByMsisdn(
            dbref: DatabaseReference,
            msisdn: String) {
        checkNotNull(msisdn)
        removeChild(dbref, stripLeadingPlus(msisdn))
    }

    @Throws(StorageException::class)
    fun removeDisplayDatastructureByMsisdn(msisdn: String) {
        removeByMsisdn(clientVisibleSubscriberRecords, msisdn)
    }

    @Throws(StorageException::class)
    fun removeSubscriberByMsisdn(msisdn: String) {
        removeByMsisdn(authorativeUserBalance, msisdn)
    }


    fun injectPurchaseRequest(pr: PurchaseRequest): String {
        val dbref = clientRequests.push()
        val crAsMap = pr.asMap()
        dbref.setValueAsync(crAsMap)
        return dbref.key
    }

    fun removeRecordOfPurchaseById(id: String) {
        removeChild(recordsOfPurchase, id)
    }


    fun removePurchaseRequestById(id: String) {
        checkNotNull(id)
        removeChild(clientRequests, id)
    }

    private fun removeChild(db: DatabaseReference, childId: String) {
        // XXX Removes whole tree, not just the subtree for id.
        //     how do I fix this?
        checkNotNull(db)
        checkNotNull(childId)
        db.child(childId).removeValueAsync()
    }

    @Throws(StorageException::class)
    fun getSubscriberFromMsisdn(msisdn: String): Subscriber? {

        val cdl = CountDownLatch(1)
        val result = HashSet<Subscriber>()

        val q = authorativeUserBalance.child(stripLeadingPlus(msisdn))

        q.addListenerForSingleValueEvent(newListenerThatWillReadSubcriberData(cdl, result))

        return waitForSubscriberData(msisdn, cdl, result)
    }

    private fun logSubscriberDataProcessing(
            msisdn: String,
            userData: String,
            result: String): String {
        val msg = ("authorativeUserBalance = '" + userData
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
        val userDataString = authorativeUserBalance.toString()
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
                logSubscriberDataProcessing(msisdn, userDataString, r.asMap().toString())
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
                if (!snapshot.exists()) {
                    cdl.countDown()
                } else {
                    val sub = snapshot.getValue(FbSubscriber::class.java)
                    val msisdn = sub.msisdn
                    if (msisdn != null) {
                        result.add(Subscriber(msisdn, sub.noOfBytesLeft))
                    }
                    cdl.countDown()
                }
            }
        }
    }

    fun updateAuthorativeUserData(sub: Subscriber) {
        checkNotNull(sub)
        checkNotNull(sub.msisdn)

        val dbref = authorativeUserBalance.child(stripLeadingPlus(sub.msisdn))
        dbref.updateChildrenAsync(sub.asMap())
    }

    fun insertNewSubscriber(sub: Subscriber) {
        checkNotNull(sub)
        checkNotNull(sub.msisdn)

        authorativeUserBalance.child(stripLeadingPlus(sub.msisdn)).setValueAsync(sub.asMap())
    }

    private fun newProductDefChangedListener(
            snapshotConsumer: Consumer<DataSnapshot>): ChildEventListener {
        return object : AbstractChildEventListener() {
            override fun onChildAdded(dataSnapshot: DataSnapshot,
                                      prevChildKey: String?) {
                snapshotConsumer.accept(dataSnapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot,
                                        previousChildName: String?) {
                snapshotConsumer.accept(snapshot)
            }
        }
    }
    
    private fun stripLeadingPlus(str: String): String {
        return str.replaceFirst("^\\+".toRegex(), "")
    }

    companion object {

        private val LOG by logger()

        private const val PHONE_NUMBER = "phoneNumber"

        private const val SECONDS_TO_WAIT_FOR_FIREBASE = 10

        private fun listenerForPurchaseRequests(
                consumer: BiFunction<String, PurchaseRequest, Unit>): AbstractChildEventListener {
            checkNotNull(consumer)
            return object : AbstractChildEventListener() {
                override fun onChildAdded(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                    if (snapshotIsInvalid(dataSnapshot)) {
                        return
                    }
                    try {
                        val req = dataSnapshot.getValue(FbPurchaseRequest::class.java)
                        consumer.apply(dataSnapshot.key,
                                PurchaseRequest(req.sku,
                                        req.paymentToken,
                                        req.msisdn,
                                        req.millisSinceEpoch,
                                        req.id))
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
                        val subscriber = child.getValue(FbSubscriber::class.java)
                        val msisdn = subscriber.msisdn;
                        if (msisdn != null) {
                            subscribers.add(Subscriber(msisdn, subscriber.noOfBytesLeft))
                        }
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
