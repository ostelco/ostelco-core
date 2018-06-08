package org.ostelco.prime.client.api.store

import io.vavr.control.Either
import io.vavr.control.Option
import org.ostelco.prime.client.api.core.ApiError
import org.ostelco.prime.client.api.model.Consent
import org.ostelco.prime.client.api.model.SubscriptionStatus
import org.ostelco.prime.logger
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.ocs.OcsSubscriberService
import org.ostelco.prime.storage.legacy.Storage
import org.ostelco.prime.storage.legacy.StorageException
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 *
 */
class SubscriberDAOImpl(private val storage: Storage, private val ocsSubscriberService: OcsSubscriberService) : SubscriberDAO {

    private val LOG by logger()

    /* Table for 'profiles'. */
    private val consentMap = ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>>()

    override fun getProfile(subscriptionId: String): Either<ApiError, Subscriber> {
        try {
            val (email, name, address, postCode, city, country) = storage.getSubscriber(subscriptionId)
                    ?: return Either.left(ApiError("Incomplete profile description"))
            return Either.right(Subscriber(
                    email,
                    name,
                    address,
                    postCode,
                    city,
                    country))
        } catch (e: StorageException) {
            LOG.error("Failed to fetch profile", e)
            return Either.left(ApiError("Failed to fetch profile"))
        }
    }

    override fun createProfile(subscriptionId: String, profile: Subscriber): Either<ApiError, Subscriber> {
        if (!SubscriberDAO.isValidProfile(profile)) {
            LOG.error("Failed to create profile. Invalid profile.")
            return Either.left(ApiError("Incomplete profile description"))
        }
        try {
            storage.addSubscriber(subscriptionId, Subscriber(
                    profile.email,
                    profile.name,
                    profile.address,
                    profile.postCode,
                    profile.city,
                    profile.country))
        } catch (e: StorageException) {
            LOG.error("Failed to create profile", e)
            return Either.left(ApiError("Failed to create profile"))
        }

        return getProfile(subscriptionId)
    }

    override fun storeApplicationToken(subscriptionId: String, token: ApplicationToken): Either<ApiError, ApplicationToken> {

        println("storeApplicationToken called")
        val result = getMsisdn(subscriptionId)

        if (result.isRight) {
            val msisdn = result.right().get()
            storage.addNotificationToken(msisdn, token.token)
            return Either.right(token)
        } else {
            return Either.left(ApiError("User not found"))
        }
    }

    override fun updateProfile(subscriptionId: String, profile: Subscriber): Either<ApiError, Subscriber> {
        if (!SubscriberDAO.isValidProfile(profile)) {
            return Either.left(ApiError("Incomplete profile description"))
        }
        try {
            storage.updateSubscriber(subscriptionId, Subscriber(
                    profile.email,
                    profile.name,
                    profile.address,
                    profile.postCode,
                    profile.city,
                    profile.country))
        } catch (e: StorageException) {
            LOG.error("Failed to update profile", e)
            return Either.left(ApiError("Failed to update profile"))
        }

        return getProfile(subscriptionId)
    }

    override fun getSubscriptionStatus(subscriptionId: String): Either<ApiError, SubscriptionStatus> {
        try {
            val balance = storage.getBalance(subscriptionId) ?: return Either.left(ApiError("No subscription data found"))
            val purchaseRecords = storage.getPurchaseRecords(subscriptionId)
            val subscriptionStatus = SubscriptionStatus(
                    balance, ArrayList(purchaseRecords))
            return Either.right(subscriptionStatus)
        } catch (e: StorageException) {
            LOG.error("Failed to get balance", e)
            return Either.left(ApiError("Failed to get balance"))
        }

    }

    override fun getMsisdn(subscriptionId: String): Either<ApiError, String> {
        var msisdn: String? = null
        try {
            msisdn = storage.getMsisdn(subscriptionId)
        } catch (e: StorageException) {
            LOG.error("Did not find msisdn for this subscription", e)
        }

        if (msisdn == null) {
            return Either.left(ApiError("Did not find subscription"))
        }
        return Either.right(msisdn)
    }

    override fun getProducts(subscriptionId: String): Either<ApiError, Collection<Product>> {
        try {
            val products = storage.getProducts()
            if (products.isEmpty()) {
                return Either.left(ApiError("No products found"))
            }
            products.forEach { key, value -> value.sku = key }
            return Either.right(products.values)

        } catch (e: StorageException) {
            LOG.error("Failed to get Products", e)
            return Either.left(ApiError("Failed to get Products"))
        }

    }

    override fun purchaseProduct(subscriptionId: String, sku: String): Option<ApiError> {
        var msisdn: String? = null
        try {
            msisdn = storage.getSubscription(subscriptionId)
        } catch (e: StorageException) {
            LOG.error("Did not find subscription", e)
        }

        if (msisdn == null) {
            return Option.of(ApiError("Did not find subscription"))
        }

        val product: Product?
        try {
            product = storage.getProduct(sku)
        } catch (e: StorageException) {
            LOG.error("Did not find product: sku = $sku", e)
            return Option.of(ApiError("Product unavailable"))
        }

        product!!.sku = sku
        val purchaseRecord = PurchaseRecord(
                msisdn,
                product,
                Instant.now().toEpochMilli())
        try {
            storage.addPurchaseRecord(subscriptionId, purchaseRecord)
        } catch (e: StorageException) {
            LOG.error("Failed to save purchase record", e)
            return Option.of(ApiError("Failed to save purchase record"))
        }

        ocsSubscriberService.topup(msisdn, sku)
        return Option.none()
    }

    override fun getConsents(subscriptionId: String): Either<ApiError, Collection<Consent>> {
        consentMap.putIfAbsent(subscriptionId, ConcurrentHashMap())
        consentMap[subscriptionId]!!.putIfAbsent("privacy", false)
        return Either.right(listOf(Consent(
                "privacy",
                "Grant permission to process personal data",
                consentMap[subscriptionId]?.get("privacy") ?: false)))
    }

    override fun acceptConsent(subscriptionId: String, consentId: String): Either<ApiError, Consent> {
        consentMap.putIfAbsent(subscriptionId, ConcurrentHashMap())
        consentMap[subscriptionId]!![consentId] = true
        return Either.right(Consent(consentId, "Grant permission to process personal data", true))
    }

    override fun rejectConsent(subscriptionId: String, consentId: String): Either<ApiError, Consent> {
        consentMap.putIfAbsent(subscriptionId, ConcurrentHashMap())
        consentMap[subscriptionId]!![consentId] = false
        return Either.right(Consent(consentId, "Grant permission to process personal data", false))
    }

    override fun reportAnalytics(subscriptionId: String, events: String): Option<ApiError> {
        return Option.none()
    }
}
