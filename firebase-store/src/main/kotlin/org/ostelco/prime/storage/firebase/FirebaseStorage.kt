package org.ostelco.prime.storage.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import org.ostelco.common.firebasex.usingCredentialsFile
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.storage.DocumentStore

/**
 * This class is using the singleton class as delegate.
 * This is done because the {@link java.util.ServiceLoader} expects public no-args constructor, which is absent in Singleton.
 */
class FirebaseStorage : DocumentStore by FirebaseStorageSingleton

object FirebaseStorageSingleton : DocumentStore {

    private val fcmTokenEntity = EntityType("notificationTokens", ApplicationToken::class.java)
    private val paymentIdEntity = EntityType("paymentId", String::class.java)

    private val firebaseDatabase = setupFirebaseInstance()

    private val fcmTokenStore = EntityStore(firebaseDatabase, fcmTokenEntity)
    private val paymentIdStore = EntityStore(firebaseDatabase, paymentIdEntity)

    private fun setupFirebaseInstance(): FirebaseDatabase {

        val config: FirebaseConfig = FirebaseConfigRegistry.firebaseConfig
        val configFile: String = config.configFile

        val options = FirebaseOptions.Builder()
                .usingCredentialsFile(configFile)
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
        return fcmTokenStore.get(applicationID) { databaseReference.child(urlEncode(msisdn)) }
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
