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

class FirebaseStorage : Storage by FirebaseStorageSingleton

object FirebaseStorageSingleton : Storage {

    override val balances: Map<String, Long>
        get() = balanceStore.getAll()

    override fun addSubscriber(id: String, subscriber: Subscriber) = subscriberStore.create(id, subscriber)

    override fun getSubscriber(id: String): Subscriber? {
        val subscriber = subscriberStore.get(id)
        subscriber?.email = id
        return subscriber
    }

    override fun updateSubscriber(id: String, subscriber: Subscriber): Boolean = subscriberStore.update(id, subscriber)

    override fun addSubscription(id: String, msisdn: String) {
        subscriptionStore.create(id, msisdn)
    }

    override fun getProduct(sku: String) = productStore.get(sku)

    override fun getProducts() = productStore.getAll()

    override fun getBalance(email: String): Long? {
        val msisdn = subscriptionStore.get(email) ?: return null
        return balanceStore.get(msisdn)
    }

    override fun setBalance(msisdn: String, noOfBytes: Long) = balanceStore.update(msisdn, noOfBytes)

    override fun addPurchaseRecord(purchase: PurchaseRecord): String? = paymentHistoryStore.add(purchase)

    override fun removeSubscriber(id: String) {
        subscriberStore.delete(id)
        val msisdn = subscriptionStore.get(id)
        if (msisdn != null) {
            subscriptionStore.delete(id)
            balanceStore.delete(msisdn)
        }
    }
}

val balanceEntity = EntityType("balance", Long::class.java)
val productEntity = EntityType("products", Product::class.java)
val subscriptionEntity = EntityType("subscriptions", String::class.java)
val subscriberEntity = EntityType("subscribers", Subscriber::class.java)
val paymentHistoryEntity = EntityType("paymentHistory", PurchaseRecord::class.java)

val config = FirebaseConfigRegistry.firebaseConfig
val firebaseDatabase = setupFirebaseInstance(config.databaseName, config.configFile)

val balanceStore = EntityStore(firebaseDatabase, balanceEntity)
val productStore = EntityStore(firebaseDatabase, productEntity)
val subscriptionStore = EntityStore(firebaseDatabase, subscriptionEntity)
val subscriberStore = EntityStore(firebaseDatabase, subscriberEntity)
val paymentHistoryStore = EntityStore(firebaseDatabase, paymentHistoryEntity)

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

const val TIMEOUT: Long = 10 //sec

class EntityType<E>(
        val path: String,
        val entityClass: Class<E>)

class EntityStore<E>(
        firebaseDatabase: FirebaseDatabase,
        private val entityType: EntityType<E>) {

    private val LOG by logger()

    private val databaseReference: DatabaseReference = firebaseDatabase.getReference("/${config.rootPath}/${entityType.path}")

    /**
     * Get Entity by Id
     *
     * @param id
     * @return Entity
     */
    fun get(id: String): E? {
        var entity: E? = null
        val countDownLatch = CountDownLatch(1);
        databaseReference.child(urlEncode(id)).addListenerForSingleValueEvent(
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
     * @return Map of <id,Entity>
     */
    fun getAll(): Map<String, E> {
        val entities: MutableMap<String, E> = LinkedHashMap()
        val countDownLatch = CountDownLatch(1);
        databaseReference.addListenerForSingleValueEvent(
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

    private fun urlEncode(value: String) =
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
     * @return id, or null
     */
    fun add(entity: E): String? {
        val newPushedEntry = databaseReference.push()
        val future = newPushedEntry.setValueAsync(entity)
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
    fun set(id: String, entity: E): Boolean {
        val future = databaseReference.child(urlEncode(id)).setValueAsync(entity)
        future.get(TIMEOUT, SECONDS) ?: return false
        return true
    }

    /**
     * Delete Entity for given id
     *
     * @return success
     */
    fun delete(id: String): Boolean {
        if (dontExists(id)) {
            return false
        }
        val future = databaseReference.child(urlEncode(id)).removeValueAsync()
        future.get(TIMEOUT, SECONDS) ?: return false
        return true
    }
}