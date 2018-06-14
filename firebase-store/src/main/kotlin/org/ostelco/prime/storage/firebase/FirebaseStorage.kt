package org.ostelco.prime.storage.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.ostelco.prime.logger
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.storage.legacy.Storage
import org.ostelco.prime.storage.legacy.StorageException
import java.io.FileInputStream
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

/**
 * This class is using the singleton class as delegate.
 * This is done because the {@link java.util.ServiceLoader} expects public no-args constructor, which is absent in Singleton.
 */
class FirebaseStorage : Storage by FirebaseStorageSingleton

object FirebaseStorageSingleton : Storage {

    private val balanceEntity = EntityType("balance", Long::class.java)
    private val productEntity = EntityType("products", Product::class.java)
    private val subscriptionEntity = EntityType("subscriptions", String::class.java)
    private val subscriberEntity = EntityType("subscribers", Subscriber::class.java)
    private val paymentHistoryEntity = EntityType("paymentHistory", PurchaseRecord::class.java)
    private val fcmTokenEntity = EntityType("notificationTokens", ApplicationToken::class.java)

    private val firebaseDatabase = setupFirebaseInstance(config.databaseName, config.configFile)

    private val balanceStore = EntityStore(firebaseDatabase, balanceEntity)
    private val productStore = EntityStore(firebaseDatabase, productEntity)
    private val subscriptionStore = EntityStore(firebaseDatabase, subscriptionEntity)
    private val subscriberStore = EntityStore(firebaseDatabase, subscriberEntity)
    private val paymentHistoryStore = EntityStore(firebaseDatabase, paymentHistoryEntity)
    private val fcmTokenStore = EntityStore(firebaseDatabase, fcmTokenEntity)

    private fun setupFirebaseInstance(
            databaseName: String,
            configFile: String): FirebaseDatabase {

        try {

            val credentials: GoogleCredentials = if (Files.exists(Paths.get(configFile))) {
                FileInputStream(configFile).use { serviceAccount -> GoogleCredentials.fromStream(serviceAccount) }
            } else {
                GoogleCredentials.getApplicationDefault()
            }

            val options = FirebaseOptions.Builder()
                    .setCredentials(credentials)
                    .setDatabaseUrl("https://$databaseName.firebaseio.com/")
                    .build()
            try {
                FirebaseApp.getInstance()
            } catch (e: Exception) {
                FirebaseApp.initializeApp(options)
            }

            return FirebaseDatabase.getInstance()

            // (un)comment next line to turn on/of extended debugging
            // from firebase.
            // this.firebaseDatabase.setLogLevel(com.google.firebase.database.Logger.Level.DEBUG);
        } catch (ex: IOException) {
            throw StorageException(ex)
        }
    }

    override val balances: Map<String, Long>
        get() = balanceStore.getAll()

    override fun addSubscriber(subscriber: Subscriber) = subscriberStore.create(subscriber.id, subscriber)

    override fun getSubscriber(id: String): Subscriber? {
        val subscriber = subscriberStore.get(id)
        subscriber?.email = id
        return subscriber
    }

    override fun updateSubscriber(subscriber: Subscriber): Boolean = subscriberStore.update(subscriber.id, subscriber)

    override fun getMsisdn(subscriptionId: String) = subscriptionStore.get(subscriptionId)

    override fun addSubscription(id: String, msisdn: String): Boolean {
        if (subscriptionStore.create(id, msisdn)) {
            // should we set non-zero default balance?
            return balanceStore.create(msisdn, 0)
        }
        return false
    }

    override fun getProduct(subscriberId: String?, sku: String) = productStore.get(sku)

    override fun getProducts(subscriberId: String): Map<String, Product> = productStore.getAll()

    override fun getBalance(id: String): Long? {
        val msisdn = subscriptionStore.get(id) ?: return null
        return balanceStore.get(msisdn)
    }

    override fun setBalance(msisdn: String, noOfBytes: Long) = balanceStore.update(msisdn, noOfBytes)

    override fun getPurchaseRecords(id: String): Collection<PurchaseRecord> {
        return paymentHistoryStore.getAll {
            // using /paymentHistory/{id} as path instead of /paymentHistor
            databaseReference.child(urlEncode(id))
        }.values
    }

    override fun addPurchaseRecord(id: String, purchase: PurchaseRecord): String? {
        return paymentHistoryStore.add(purchase) {
            // using /paymentHistory/{id} as path instead of /paymentHistor
            databaseReference.child(urlEncode(id))
        }
    }

    override fun removeSubscriber(id: String): Boolean {
        subscriberStore.delete(id)
        // for payment history, skip checking if it exists.
        paymentHistoryStore.delete(id, dontExists = false)
        val msisdn = subscriptionStore.get(id)
        if (msisdn != null) {
            if (subscriptionStore.delete(id)) {
                return balanceStore.delete(msisdn)
            }
        }
        return false
    }

    override fun addNotificationToken(msisdn: String, token: ApplicationToken) : Boolean {
        return fcmTokenStore.set(token.applicationID, token) { databaseReference.child(urlEncode(msisdn)) }
    }

    override fun getNotificationToken(msisdn: String, applicationID: String): ApplicationToken? {
        return fcmTokenStore.get(applicationID) { databaseReference.child(msisdn) }
    }

    override fun getNotificationTokens(msisdn: String): Collection<ApplicationToken> {
        return fcmTokenStore.getAll {
            databaseReference.child(urlEncode(msisdn))
        }.values
    }

    override fun removeNotificationToken(msisdn: String, applicationID: String): Boolean {
        return fcmTokenStore.delete(applicationID) { databaseReference.child(urlEncode(msisdn)) }
    }
}

private val config = FirebaseConfigRegistry.firebaseConfig

const val TIMEOUT: Long = 10 //sec

class EntityType<E>(
        val path: String,
        val entityClass: Class<E>)

class EntityStore<E>(
        firebaseDatabase: FirebaseDatabase,
        private val entityType: EntityType<E>) {

    private val LOG by logger()

    val databaseReference: DatabaseReference = firebaseDatabase.getReference("/${config.rootPath}/${entityType.path}")

    /**
     * Get Entity by Id
     *
     * @param id
     * @return Entity
     */
    fun get(id: String, reference: EntityStore<E>.() -> DatabaseReference = { databaseReference }): E? {
        var entity: E? = null
        val countDownLatch = CountDownLatch(1);
        reference().child(urlEncode(id)).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError?) {
                        countDownLatch.countDown()
                    }

                    override fun onDataChange(snapshot: DataSnapshot?) {
                        if (snapshot != null) {
                            entity = snapshot.getValue(entityType.entityClass)
                        }
                        countDownLatch.countDown()
                    }
                })
        countDownLatch.await(TIMEOUT, SECONDS)
        return entity
    }

    /**
     * Get all Entities
     *
     * @param reference This is a Function which returns DatabaseReference to be used.
     *                  Default value is a function which returns base path for that EntityType.
     *                  For special cases, it allows to use child path for getAll operation.
     *
     * @return Map of <id,Entity>
     */
    fun getAll(reference: EntityStore<E>.() -> DatabaseReference = { databaseReference }): Map<String, E> {
        val entities: MutableMap<String, E> = LinkedHashMap()
        val countDownLatch = CountDownLatch(1);
        reference().addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError?) {
                        countDownLatch.countDown()
                    }

                    override fun onDataChange(snapshot: DataSnapshot?) {
                        if (snapshot != null) {
                            for (child in snapshot.children) {
                                val value = child.getValue(entityType.entityClass)
                                entities.put(urlDecode(child.key), value)
                            }
                        }
                        countDownLatch.countDown()
                    }
                })
        countDownLatch.await(TIMEOUT, SECONDS)
        return entities
    }

    fun urlEncode(value: String) =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace(oldValue = ".", newValue = "%2E")

    private fun urlDecode(value: String) =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace(oldValue = "%2E", newValue = ".")

    /**
     * Check if entity exists for a given value
     */
    fun exists(id: String) = get(id) != null

    /**
     * Inverse of exists
     */
    fun dontExists(id: String) = !exists(id)

    /**
     * Create Entity for given id
     *
     * @return success
     */
    fun create(id: String, entity: E): Boolean {
        // fail if already exist
        if (exists(id)) {
            return false
        }
        return set(id, entity)
    }

    /**
     * Create Entity with auto-gen id, or null
     *
     * @param entity Entity to be added
     * @param reference This is a Function which returns DatabaseReference to be used.
     *                  Default value is a function which returns base path for that EntityType.
     *                  For special cases, it allows to use child path for add operation.
     *
     *
     * @return id, or null
     */
    fun add(entity: E, reference: EntityStore<E>.() -> DatabaseReference = { databaseReference }): String? {
        val newPushedEntry = reference().push()
        val future = newPushedEntry.setValueAsync(entity)
        // FIXME this may always return null
        future.get(TIMEOUT, SECONDS) ?: return null
        return newPushedEntry.key
    }

    /**
     * Update Entity for given id
     *
     * @return success
     */
    fun update(id: String, entity: E): Boolean {
        if (dontExists(id)) {
            return false
        }
        return set(id, entity)
    }

    /**
     * Set Entity for given id
     *
     * @return success
     */
    fun set(id: String, entity: E, reference: EntityStore<E>.() -> DatabaseReference = { databaseReference }): Boolean {
        val future = reference().child(urlEncode(id)).setValueAsync(entity)
        // FIXME this always return false
        future.get(TIMEOUT, SECONDS) ?: return false
        return true
    }

    /**
     * Delete Entity for given id
     *
     * @param id
     * @param dontExists Optional parameter if you want to skip checking if entry exists.
     *
     * @return success
     */
    fun delete(id: String, dontExists: Boolean = dontExists(id), reference: EntityStore<E>.() -> DatabaseReference = { databaseReference }): Boolean {
        if (dontExists) {
            return false
        }
        val future = reference().child(urlEncode(id)).removeValueAsync()
        // FIXME this may always return false
        future.get(TIMEOUT, SECONDS) ?: return false
        return true
    }
}