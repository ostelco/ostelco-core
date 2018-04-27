package org.ostelco.prime.firebase

import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Preconditions.checkNotNull
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseCredentials
import com.google.firebase.database.FirebaseDatabase
import org.ostelco.prime.events.EventListeners
import org.ostelco.prime.storage.ProductDescriptionCache
import org.ostelco.prime.storage.ProductDescriptionCacheImpl
import org.ostelco.prime.storage.PurchaseRequestHandler
import org.ostelco.prime.storage.Storage
import org.ostelco.prime.storage.StorageException
import org.ostelco.prime.storage.entities.Product
import org.ostelco.prime.storage.entities.PurchaseRequest
import org.ostelco.prime.storage.entities.Subscriber
import org.ostelco.prime.storage.entities.SubscriberImpl
import java.io.FileInputStream
import java.io.IOException
import java.util.function.BiFunction
import java.util.function.Consumer

class FbStorage @Throws(StorageException::class)
constructor(databaseName: String,
            configFile: String,
            listeners: EventListeners) : Storage {

    private val productCache: ProductDescriptionCache

    private val facade: FbDatabaseFacade

    private val listeners: EventListeners

    override val allSubscribers: Collection<Subscriber>
        get() = facade.allSubscribers

    init {

        checkNotNull(configFile)
        checkNotNull(databaseName)
        this.listeners = checkNotNull(listeners)

        this.productCache = ProductDescriptionCacheImpl

        val firebaseDatabase = setupFirebaseInstance(databaseName, configFile)

        this.facade = FbDatabaseFacade(firebaseDatabase)

        facade.addProductCatalogItemListener(Consumer { listeners.productCatalogItemListener(it) })

        // Load subscriber balance from firebase to in-memory OcsState
        listeners.loadSubscriberBalanceDataFromFirebaseToInMemoryStructure(allSubscribers)
    }

    override fun addTopupProduct(sku: String, noOfBytes: Long) {
        productCache.addTopupProduct(sku, noOfBytes)
    }

    override fun isValidSKU(sku: String): Boolean {
        return productCache.isValidSKU(sku)
    }

    override fun getProductForSku(sku: String): Product? {
        return productCache.getProductForSku(sku)
    }

    @Throws(StorageException::class)
    private fun setupFirebaseInstance(
            databaseName: String,
            configFile: String): FirebaseDatabase {
        try {
            FileInputStream(configFile).use { serviceAccount ->

                val options = FirebaseOptions.Builder().setCredential(FirebaseCredentials.fromCertificate(serviceAccount)).setDatabaseUrl("https://$databaseName.firebaseio.com/").build()

                try {
                    FirebaseApp.getInstance()
                } catch (e: Exception) {
                    FirebaseApp.initializeApp(options)
                }

                return FirebaseDatabase.getInstance()

                // (un)comment next line to turn on/of extended debugging
                // from firebase.
                // this.firebaseDatabase.setLogLevel(com.google.firebase.database.Logger.Level.DEBUG);

            }
        } catch (ex: IOException) {
            throw StorageException(ex)
        }

    }


    // XXX This method represents a bad design decision.  It's too circumspect to
    //     understand.  Fix!
    override fun addPurchaseRequestHandler(handler: PurchaseRequestHandler) {
        checkNotNull(handler)
        listeners.addPurchaseRequestHandler(handler)
        facade.addPurchaseRequestListener(BiFunction { key, req -> listeners.purchaseRequestListener(key, req) })
    }

    @Throws(StorageException::class)
    override fun updateDisplayDatastructure(msisdn: String) {
        checkNotNull(msisdn)
        val subscriber = getSubscriberFromMsisdn(msisdn) as SubscriberImpl?
                ?: throw StorageException("Unknown MSISDN " + msisdn)

        val noOfBytes = subscriber.noOfBytesLeft
        val noOfGBLeft = noOfBytes / 1.0E09f
        val gbLeft = String.format("%.2f GB", noOfGBLeft)

        facade.updateClientVisibleUsageString(msisdn, gbLeft)
    }

    @Throws(StorageException::class)
    override fun removeDisplayDatastructure(msisdn: String) {
        checkNotNull(msisdn)
        facade.removeByMsisdn(msisdn)
    }

    override fun injectPurchaseRequest(pr: PurchaseRequest): String {
        checkNotNull(pr)
        return facade.injectPurchaseRequest(pr)
    }

    override fun removeRecordOfPurchaseById(id: String) {
        checkNotNull(id)
        facade.removeRecordOfPurchaseById(id)
    }

    override fun addRecordOfPurchaseByMsisdn(
            ephermeralMsisdn: String,
            sku: String,
            now: Long): String {
        checkNotNull(ephermeralMsisdn)
        checkNotNull(sku)
        checkArgument(now > 0)

        return facade.addRecordOfPurchaseByMsisdn(ephermeralMsisdn, sku, now)
    }

    @Throws(StorageException::class)
    override fun removeSubscriberByMsisdn(msisdn: String) {
        checkNotNull(msisdn)
        facade.removeSubscriberByMsisdn(msisdn)
    }

    override fun removePurchaseRequestById(id: String) {
        checkNotNull(id)
        facade.removePurchaseRequestById(id)
    }

    @Throws(StorageException::class)
    override fun getSubscriberFromMsisdn(msisdn: String): Subscriber? {
        checkNotNull(msisdn)
        return facade.getSubscriberFromMsisdn(msisdn)
    }

    @Throws(StorageException::class)
    override fun setRemainingByMsisdn(
            msisdn: String?,
            noOfBytes: Long) {

        if (msisdn == null) {
            throw StorageException("msisdn can't be null")
        }

        if (noOfBytes < 0) {
            throw StorageException("noOfBytes can't be negative")
        }

        val sub = SubscriberImpl(msisdn)
        sub.setNoOfBytesLeft(noOfBytes)

        facade.updateAuthorativeUserData(sub)
    }

    override fun insertNewSubscriber(msisdn: String) {
        checkNotNull(msisdn)
        val sub = SubscriberImpl(msisdn)
        return facade.insertNewSubscriber(sub)
    }
}
