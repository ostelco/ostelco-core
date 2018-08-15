package org.ostelco.prime.client.api.store

import arrow.core.Either
import arrow.core.flatMap
import org.ostelco.prime.client.api.model.Consent
import org.ostelco.prime.client.api.model.Person
import org.ostelco.prime.client.api.model.SubscriptionStatus
import org.ostelco.prime.core.ApiError
import org.ostelco.prime.core.BadGatewayError
import org.ostelco.prime.core.NotFoundError
import org.ostelco.prime.logger
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.module.getResource
import org.ostelco.prime.ocs.OcsSubscriberService
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.paymentprocessor.core.ProfileInfo
import org.ostelco.prime.storage.ClientDataSource
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 *
 */
class SubscriberDAOImpl(private val storage: ClientDataSource, private val ocsSubscriberService: OcsSubscriberService) : SubscriberDAO {

    private val logger by logger()

    private val paymentProcessor by lazy { getResource<PaymentProcessor>() }

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
            return storage.addSubscriber(profile, referredBy)
                    .mapLeft { ApiError("Failed to create profile. ${it.message}") }
                    .flatMap { getProfile(subscriberId) }
        } catch (e: Exception) {
            logger.error("Failed to create profile", e)
            return Either.left(ApiError("Failed to create profile"))
        }
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
                    .flatMap { balance ->
                        storage.getPurchaseRecords(subscriberId)
                                .map { purchaseRecords -> SubscriptionStatus(balance, purchaseRecords.toList()) }
                    }
                    .mapLeft { ApiError(it.message) }
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

    override fun getProduct(subscriptionId: String, sku: String): Either<ApiError, Product> {
        return storage.getProduct(subscriptionId, sku)
                .fold({ Either.left(ApiError("Failed to get products for sku $sku")) },
                        { Either.right(it) })
    }

    private fun createAndStorePaymentProfile(name: String): Either<ApiError, ProfileInfo> {
        return paymentProcessor.createPaymentProfile(name)
                .flatMap { profileInfo ->
                    setPaymentProfile(name, profileInfo)
                            .map { profileInfo }
                }
    }

    // TODO: extend ApiError to allow more information, so that we can avoid returning Pair<Response.Status, ApiError>
    override fun purchaseProduct(subscriberId: String, sku: String, sourceId: String?, saveCard: Boolean): Either<ApiError, ProductInfo> {
        return getProduct(subscriberId, sku)
                // If we can't find the product, return not-found
                .mapLeft { NotFoundError("Product unavailable") }
                .flatMap { product ->
                    product.sku = sku
                    val purchaseRecord = PurchaseRecord(
                            product = product,
                            timestamp = Instant.now().toEpochMilli())
                            // Create purchase record
                    storage.addPurchaseRecord(subscriberId, purchaseRecord)
                            .mapLeft {
                                BadGatewayError("Failed to save purchase record")
                            }
                            // Notify OCS
                            .flatMap {
                                //TODO: Handle errors (when it becomes available)
                                ocsSubscriberService.topup(subscriberId, sku)
                                Either.right(product)
                            }
                }
                .flatMap { product: Product ->
                    // Fetch/Create stripe payment profile for the subscriber.
                    getPaymentProfile(subscriberId)
                            .fold(
                                    { createAndStorePaymentProfile(subscriberId) },
                                    { apiError -> Either.right(apiError) }
                            )
                            .mapLeft { apiError -> BadGatewayError(apiError.description) }
                            .map { profileInfo -> Pair(product, profileInfo) }
                }
                .flatMap {  (product, profileInfo) ->
                    // Create stripe charge for this purchase
                    val price = product.price
                    val customerId = profileInfo.id
                    val result: Either<ApiError, ProductInfo> = if (sourceId != null) {
                        paymentProcessor.chargeUsingSource(customerId, sourceId, price.amount, price.currency, saveCard)
                    } else {
                        paymentProcessor.chargeUsingDefaultSource(customerId, price.amount, price.currency)
                    }
                    result.mapLeft { BadGatewayError(it.description) }
                }
    }

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

    override fun getPaymentProfile(name: String): Either<ApiError, ProfileInfo> =
            storage.getPaymentId(name)
                    ?.let { profileInfoId -> Either.right(ProfileInfo(profileInfoId)) }
                    ?: Either.left(ApiError("Failed to fetch payment customer ID"))

    override fun setPaymentProfile(name: String, profileInfo: ProfileInfo): Either<ApiError, Unit> =
        Either.cond(
                test = storage.createPaymentId(name, profileInfo.id),
                ifTrue = { Unit },
                ifFalse = { ApiError("Failed to save payment customer ID") })

    override fun reportAnalytics(subscriberId: String, events: String): Either<ApiError, Unit> = Either.right(Unit)
}
