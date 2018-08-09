package org.ostelco.prime.client.api.store

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.flatMap
import org.ostelco.prime.client.api.core.ApiError
import org.ostelco.prime.client.api.model.Consent
import org.ostelco.prime.client.api.model.Person
import org.ostelco.prime.client.api.model.SubscriptionStatus
import org.ostelco.prime.logger
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.ocs.OcsSubscriberService
import org.ostelco.prime.storage.ClientDataSource
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 *
 */
class SubscriberDAOImpl(private val storage: ClientDataSource, private val ocsSubscriberService: OcsSubscriberService) : SubscriberDAO {

    private val logger by logger()

    /* Table for 'profiles'. */
    private val consentMap = ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>>()

    override fun getProfile(subscriberId: String): Either<ApiError, Subscriber> {
        return try {
            storage.getSubscriber(subscriberId).mapLeft {
                        ApiError("Incomplete profile description. ${it.message}")
                    }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile", e)
            Either.left(ApiError("Failed to fetch profile"))
        }
    }

    override fun createProfile(subscriberId: String, profile: Subscriber, referredBy: String?): Either<ApiError, Subscriber> {
        if (!SubscriberDAO.isValidProfile(profile)) {
            logger.error("Failed to create profile. Invalid profile.")
            return Either.left(ApiError("Incomplete profile description"))
        }
        try {
            profile.referralId = profile.email
            storage.addSubscriber(profile, referredBy)
        } catch (e: Exception) {
            logger.error("Failed to create profile", e)
            return Either.left(ApiError("Failed to create profile"))
        }

        return getProfile(subscriberId)
    }

    override fun storeApplicationToken(msisdn: String, applicationToken: ApplicationToken): Either<ApiError, ApplicationToken> {

        if (!SubscriberDAO.isValidApplicationToken(applicationToken)) {
            return Either.left(ApiError("Incomplete ApplicationToken"))
        }

        try {
            storage.addNotificationToken(msisdn, applicationToken)
        } catch (e: Exception) {
            logger.error("Failed to store ApplicationToken", e)
            return Either.left(ApiError("Failed to store ApplicationToken"))
        }
        return getNotificationToken(msisdn, applicationToken.applicationID)
    }

    private fun getNotificationToken(msisdn: String, applicationId: String): Either<ApiError, ApplicationToken> {
        try {
            return storage.getNotificationToken(msisdn, applicationId)
                    ?.let { Either.right(it) }
                    ?: return Either.left(ApiError("Failed to get ApplicationToken"))
        } catch (e: Exception) {
            logger.error("Failed to get ApplicationToken", e)
            return Either.left(ApiError("Failed to get ApplicationToken"))
        }
    }

    override fun updateProfile(subscriberId: String, profile: Subscriber): Either<ApiError, Subscriber> {
        if (!SubscriberDAO.isValidProfile(profile)) {
            return Either.left(ApiError("Incomplete profile description"))
        }
        try {
            profile.referralId = profile.email
            storage.updateSubscriber(profile)
        } catch (e: Exception) {
            logger.error("Failed to update profile", e)
            return Either.left(ApiError("Failed to update profile"))
        }

        return getProfile(subscriberId)
    }

    override fun getSubscriptionStatus(subscriberId: String): Either<ApiError, SubscriptionStatus> {
        try {
            return storage.getBundles(subscriberId)
                    .map { bundles -> bundles?.first()?.balance ?: 0 }
                    .map { balance ->
                        storage.getPurchaseRecords(subscriberId)
                                .map { purchaseRecords -> SubscriptionStatus(balance, purchaseRecords.toList()) }
                                .mapLeft { ApiError(it.message) }
                    }
                    .mapLeft { ApiError(it.message) }
                    .flatMap { it }
        } catch (e: Exception) {
            logger.error("Failed to get balance", e)
            return Either.left(ApiError("Failed to get balance"))
        }
    }

    override fun getSubscriptions(subscriberId: String): Either<ApiError, Collection<Subscription>> {
        try {
            return storage.getSubscriptions(subscriberId).mapLeft {
                ApiError("Failed to get subscriptions. ${it.message}")
            }
        } catch (e: Exception) {
            logger.error("Failed to get subscriptions", e)
            return Either.left(ApiError("Failed to get subscriptions"))
        }
    }

    override fun getPurchaseHistory(subscriberId: String): Either<ApiError, Collection<PurchaseRecord>> {
        return try {
            return storage.getPurchaseRecords(subscriberId).bimap(
                    { ApiError("Failed to get purchase history. ${it.message}") },
                    { it.toList() })
        } catch (e: Exception) {
            logger.error("Failed to get purchase history", e)
            Either.left(ApiError("Failed to get purchase history"))
        }
    }

    override fun getMsisdn(subscriberId: String): Either<ApiError, String> {
        try {
            return storage.getMsisdn(subscriberId).mapLeft {
                ApiError("Did not find msisdn for this subscription. ${it.message}")
            }
        } catch (e: Exception) {
            logger.error("Did not find msisdn for this subscription", e)
            return Either.left(ApiError("Did not find subscription"))
        }
    }

    override fun getProducts(subscriberId: String): Either<ApiError, Collection<Product>> {
        try {
            return storage.getProducts(subscriberId).bimap(
                    { ApiError(it.message) },
                    { products ->
                        products.forEach { key, value -> value.sku = key }
                        products.values
                    })
        } catch (e: Exception) {
            logger.error("Failed to get Products", e)
            return Either.left(ApiError("Failed to get Products"))
        }

    }

    override fun purchaseProduct(subscriberId: String, sku: String): Option<ApiError> =
            storage.getProduct(subscriberId, sku).fold(
                    {
                        logger.error("Did not find product: sku = $sku")
                        Option(ApiError("Product unavailable"))
                    },
                    { product ->
                        product.sku = sku
                        val purchaseRecord = PurchaseRecord(
                                product = product,
                                timestamp = Instant.now().toEpochMilli())
                        storage.addPurchaseRecord(subscriberId, purchaseRecord)
                                .swap()
                                .toOption()
                                .map {
                                    logger.error("Failed to save purchase record")
                                    Some(ApiError("Failed to save purchase record"))
                                }
                        ocsSubscriberService.topup(subscriberId, sku)
                        None
                    })


    override fun getReferrals(subscriberId: String): Either<ApiError, Collection<Person>> {
        return try {
            storage.getReferrals(subscriberId).bimap(
                    { ApiError("Failed to get referral list. ${it.message}") },
                    { list -> list.map { Person(it) } })
        } catch (e: Exception) {
            logger.error("Failed to get referral list", e)
            Either.left(ApiError("Failed to get referral list"))
        }
    }

    override fun getReferredBy(subscriberId: String): Either<ApiError, Person> {
        return try {
            storage.getReferredBy(subscriberId).bimap(
                    { ApiError("Failed to get referred-by. ${it.message}") },
                    { Person(name = it) })
        } catch (e: Exception) {
            logger.error("Failed to get referred-by", e)
            Either.left(ApiError("Failed to get referred-by"))
        }
    }

    override fun getConsents(subscriberId: String): Either<ApiError, Collection<Consent>> {
        consentMap.putIfAbsent(subscriberId, ConcurrentHashMap())
        consentMap[subscriberId]!!.putIfAbsent("privacy", false)
        return Either.right(listOf(Consent(
                consentId = "privacy",
                description = "Grant permission to process personal data",
                accepted = consentMap[subscriberId]?.get("privacy") ?: false)))
    }

    override fun acceptConsent(subscriberId: String, consentId: String): Either<ApiError, Consent> {
        consentMap.putIfAbsent(subscriberId, ConcurrentHashMap())
        consentMap[subscriberId]!![consentId] = true
        return Either.right(Consent(consentId, "Grant permission to process personal data", true))
    }

    override fun rejectConsent(subscriberId: String, consentId: String): Either<ApiError, Consent> {
        consentMap.putIfAbsent(subscriberId, ConcurrentHashMap())
        consentMap[subscriberId]!![consentId] = false
        return Either.right(Consent(consentId, "Grant permission to process personal data", false))
    }

    override fun reportAnalytics(subscriberId: String, events: String): Option<ApiError> = None
}
