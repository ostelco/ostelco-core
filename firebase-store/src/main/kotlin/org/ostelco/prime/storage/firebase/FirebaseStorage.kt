package org.ostelco.prime.storage.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import org.ostelco.prime.logger
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.storage.DocumentStore
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths

/**
 * This class is using the singleton class as delegate.
 * This is done because the {@link java.util.ServiceLoader} expects public no-args constructor, which is absent in Singleton.
 */
class FirebaseStorage : DocumentStore by FirebaseStorageSingleton

object FirebaseStorageSingleton : DocumentStore {

    private val LOG by logger()

    private val balanceEntity = EntityType("balance", Long::class.java)
    private val productEntity = EntityType("products", Product::class.java)
    private val subscriptionEntity = EntityType("subscriptions", String::class.java)
    private val subscriberEntity = EntityType("subscribers", Subscriber::class.java)
    private val paymentHistoryEntity = EntityType("paymentHistory", PurchaseRecord::class.java)
    private val fcmTokenEntity = EntityType("notificationTokens", ApplicationToken::class.java)
    private val paymentIdEntity = EntityType("paymentId", String::class.java)

    private val firebaseDatabase = setupFirebaseInstance()

    private val balanceStore = EntityStore(firebaseDatabase, balanceEntity)
    private val productStore = EntityStore(firebaseDatabase, productEntity)
    private val subscriptionStore = EntityStore(firebaseDatabase, subscriptionEntity)
    private val subscriberStore = EntityStore(firebaseDatabase, subscriberEntity)
    private val paymentHistoryStore = EntityStore(firebaseDatabase, paymentHistoryEntity)
    private val fcmTokenStore = EntityStore(firebaseDatabase, fcmTokenEntity)
    private val paymentIdStore = EntityStore(firebaseDatabase, paymentIdEntity)

    private fun setupFirebaseInstance(): FirebaseDatabase {

        val config: FirebaseConfig = FirebaseConfigRegistry.firebaseConfig
        val databaseName: String = config.databaseName
        val configFile: String = config.configFile

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
    }

    override fun addNotificationToken(msisdn: String, token: ApplicationToken): Boolean {
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

    override fun getPaymentId(id: String): String? = paymentIdStore.get(id)

    override fun deletePaymentId(id: String): Boolean = paymentIdStore.delete(id)

    override fun createPaymentId(id: String, paymentId: String): Boolean = paymentIdStore.create(id, paymentId)
}