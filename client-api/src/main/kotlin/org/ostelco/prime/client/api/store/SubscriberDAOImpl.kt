package org.ostelco.prime.client.api.store

import arrow.core.Either
import arrow.core.flatMap
import org.ostelco.prime.analytics.AnalyticsService
import org.ostelco.prime.analytics.PrimeMetric.REVENUE
import org.ostelco.prime.client.api.metrics.updateMetricsOnNewSubscriber
import org.ostelco.prime.client.api.model.Consent
import org.ostelco.prime.client.api.model.Person
import org.ostelco.prime.client.api.model.SubscriptionStatus
import org.ostelco.prime.core.ApiError
import org.ostelco.prime.core.BadGatewayError
import org.ostelco.prime.core.BadRequestError
import org.ostelco.prime.core.ForbiddenError
import org.ostelco.prime.core.InsuffientStorageError
import org.ostelco.prime.core.NotFoundError
import org.ostelco.prime.logger
import org.ostelco.prime.model.ActivePseudonyms
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
import org.ostelco.prime.paymentprocessor.core.SourceInfo
import org.ostelco.prime.pseudonymizer.PseudonymizerService
import org.ostelco.prime.storage.ClientDataSource
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 *
 */
class SubscriberDAOImpl(private val storage: ClientDataSource, private val ocsSubscriberService: OcsSubscriberService) : SubscriberDAO {

    private val logger by logger()

    private val paymentProcessor by lazy { getResource<PaymentProcessor>() }
    private val pseudonymizer by lazy { getResource<PseudonymizerService>() }
    private val analyticsReporter by lazy { getResource<AnalyticsService>() }

    /* Table for 'profiles'. */
    private val consentMap = ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>>()

    override fun getProfile(subscriberId: String): Either<ApiError, Subscriber> {
        return try {
            storage.getSubscriber(subscriberId).mapLeft {
                        BadRequestError("Incomplete profile description. ${it.message}")
                    }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile for subscriberId $subscriberId", e)
            Either.left(NotFoundError("Failed to fetch profile"))
        }
    }

    override fun createProfile(subscriberId: String, profile: Subscriber, referredBy: String?): Either<ApiError, Subscriber> {
        if (!SubscriberDAO.isValidProfile(profile)) {
            logger.error("Failed to create profile. Invalid profile.")
            return Either.left(BadRequestError("Incomplete profile description"))
        }
        return try {
            storage.addSubscriber(profile, referredBy)
                    .mapLeft { ForbiddenError("Failed to create profile. ${it.message}") }
                    .flatMap {
                        updateMetricsOnNewSubscriber()
                        getProfile(subscriberId)
                    }
        } catch (e: Exception) {
            logger.error("Failed to create profile for subscriberId $subscriberId", e)
            Either.left(ForbiddenError("Failed to create profile"))
        }
    }

    override fun storeApplicationToken(msisdn: String, applicationToken: ApplicationToken): Either<ApiError, ApplicationToken> {

        if (!SubscriberDAO.isValidApplicationToken(applicationToken)) {
            return Either.left(BadRequestError("Incomplete ApplicationToken"))
        }

        try {
            storage.addNotificationToken(msisdn, applicationToken)
        } catch (e: Exception) {
            logger.error("Failed to store ApplicationToken for msisdn $msisdn", e)
            return Either.left(InsuffientStorageError("Failed to store ApplicationToken"))
        }
        return getNotificationToken(msisdn, applicationToken.applicationID)
    }

    private fun getNotificationToken(msisdn: String, applicationId: String): Either<ApiError, ApplicationToken> {
        try {
            return storage.getNotificationToken(msisdn, applicationId)
                    ?.let { Either.right(it) }
                    ?: return Either.left(NotFoundError("Failed to get ApplicationToken"))
        } catch (e: Exception) {
            logger.error("Failed to get ApplicationToken for msisdn $msisdn", e)
            return Either.left(NotFoundError("Failed to get ApplicationToken"))
        }
    }

    override fun updateProfile(subscriberId: String, profile: Subscriber): Either<ApiError, Subscriber> {
        if (!SubscriberDAO.isValidProfile(profile)) {
            return Either.left(BadRequestError("Incomplete profile description"))
        }
        try {
            storage.updateSubscriber(profile)
        } catch (e: Exception) {
            logger.error("Failed to update profile for subscriberId $subscriberId", e)
            return Either.left(NotFoundError("Failed to update profile"))
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
                    .mapLeft { NotFoundError(it.message) }
        } catch (e: Exception) {
            logger.error("Failed to get balance for subscriber $subscriberId", e)
            return Either.left(NotFoundError("Failed to get balance"))
        }
    }

    override fun getSubscriptions(subscriberId: String): Either<ApiError, Collection<Subscription>> {
        try {
            return storage.getSubscriptions(subscriberId).mapLeft {
               NotFoundError("Failed to get subscriptions. ${it.message}")
            }
        } catch (e: Exception) {
            logger.error("Failed to get subscriptions for subscriberId $subscriberId", e)
            return Either.left(NotFoundError("Failed to get subscriptions"))
        }
    }

    override fun getActivePseudonymOfMsisdnForSubscriber(subscriberId: String): Either<ApiError, ActivePseudonyms> {
        return storage.getMsisdn(subscriberId)
                .mapLeft { NotFoundError("Failed to msisdn for user. ${it.message}") }
                .map { msisdn -> pseudonymizer.getActivePseudonymsForMsisdn(msisdn) }
    }

    override fun getPurchaseHistory(subscriberId: String): Either<ApiError, Collection<PurchaseRecord>> {
        return try {
            return storage.getPurchaseRecords(subscriberId).bimap(
                    { NotFoundError("Failed to get purchase history. ${it.message}") },
                    { it.toList() })
        } catch (e: Exception) {
            logger.error("Failed to get purchase history for subscriberId $subscriberId", e)
            Either.left(NotFoundError("Failed to get purchase history"))
        }
    }

    override fun getMsisdn(subscriberId: String): Either<ApiError, String> {
        return try {
            storage.getMsisdn(subscriberId).mapLeft {
                NotFoundError("Did not find msisdn for this subscription. ${it.message}")
            }
        } catch (e: Exception) {
            logger.error("Did not find msisdn for subscriberId $subscriberId", e)
            Either.left(NotFoundError("Did not find subscription"))
        }
    }

    override fun getProducts(subscriberId: String): Either<ApiError, Collection<Product>> {
        return try {
            storage.getProducts(subscriberId).bimap(
                    { NotFoundError(it.message) },
                    { products -> products.values })
        } catch (e: Exception) {
            logger.error("Failed to get Products for subscriberId $subscriberId", e)
            Either.left(NotFoundError("Failed to get Products"))
        }

    }

    override fun getProduct(subscriptionId: String, sku: String): Either<ApiError, Product> {
        return storage.getProduct(subscriptionId, sku)
                .fold({ Either.left(NotFoundError("Failed to get products for sku $sku")) },
                        { Either.right(it) })
    }

    private fun createAndStorePaymentProfile(name: String): Either<ApiError, ProfileInfo> {
        return paymentProcessor.createPaymentProfile(name)
                .mapLeft { ForbiddenError(it.description) }
                .flatMap { profileInfo ->
                    setPaymentProfile(name, profileInfo)
                            .map { profileInfo }
                }
    }

    @Deprecated("use purchaseProduct", ReplaceWith("purchaseProduct"))
    override fun purchaseProductWithoutPayment(subscriberId: String, sku: String): Either<ApiError,Unit> {
        return getProduct(subscriberId, sku)
                // If we can't find the product, return not-found
                .mapLeft { NotFoundError("Product unavailable") }
                .flatMap { product ->
                    val purchaseRecord = PurchaseRecord(
                            id = UUID.randomUUID().toString(),
                            product = product,
                            timestamp = Instant.now().toEpochMilli(),
                            msisdn = "")
                    // Create purchase record
                    storage.addPurchaseRecord(subscriberId, purchaseRecord)
                            .mapLeft { storeError ->
                                logger.error("failed to save purchase record, for subscriberId $subscriberId, sku $sku")
                                BadGatewayError(storeError.message)
                            }
                            // Notify OCS
                            .flatMap {
                                //TODO: Handle errors (when it becomes available)
                                ocsSubscriberService.topup(subscriberId, sku)
                                // TODO vihang: handle currency conversion
                                analyticsReporter.reportMetric(REVENUE, product.price.amount.toLong())
                                Either.right(Unit)
                            }
                }
    }

    override fun purchaseProduct(
            subscriberId: String,
            sku: String,
            sourceId: String?,
            saveCard: Boolean): Either<ApiError, ProductInfo> =
        storage.purchaseProduct(
                subscriberId,
                sku,
                sourceId,
                saveCard).mapLeft { NotFoundError(it.description) }

    override fun getReferrals(subscriberId: String): Either<ApiError, Collection<Person>> {
        return try {
            storage.getReferrals(subscriberId).bimap(
                    { NotFoundError("Failed to get referral list. ${it.message}") },
                    { list -> list.map { Person(it) } })
        } catch (e: Exception) {
            logger.error("Failed to get referral list for subscriberId $subscriberId", e)
            Either.left(NotFoundError("Failed to get referral list"))
        }
    }

    override fun getReferredBy(subscriberId: String): Either<ApiError, Person> {
        return try {
            storage.getReferredBy(subscriberId).bimap(
                    { NotFoundError("Failed to get referred-by. ${it.message}") },
                    { Person(name = it) })
        } catch (e: Exception) {
            logger.error("Failed to get referred-by for subscriberId $subscriberId", e)
            Either.left(NotFoundError("Failed to get referred-by"))
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
                    ?: Either.left(BadGatewayError("Failed to fetch payment customer ID"))

    override fun setPaymentProfile(name: String, profileInfo: ProfileInfo): Either<ApiError, Unit> =
        Either.cond(
                test = storage.createPaymentId(name, profileInfo.id),
                ifTrue = { Unit },
                ifFalse = { BadGatewayError("Failed to save payment customer ID") })

    override fun reportAnalytics(subscriberId: String, events: String): Either<ApiError, Unit> = Either.right(Unit)

    override fun createSource(subscriberId: String, sourceId: String): Either<ApiError, SourceInfo> {
        return getPaymentProfile(subscriberId)
                .fold(
                        { createAndStorePaymentProfile(subscriberId) },
                        { profileInfo -> Either.right(profileInfo) }
                )
                .flatMap { profileInfo -> paymentProcessor.addSource(profileInfo.id, sourceId).mapLeft { NotFoundError(it.description) } }
    }

    override fun setDefaultSource(subscriberId: String, sourceId: String): Either<ApiError, SourceInfo> {
        return getPaymentProfile(subscriberId)
                .fold(
                        { createAndStorePaymentProfile(subscriberId) },
                        { profileInfo -> Either.right(profileInfo) }
                )
                .flatMap { profileInfo -> paymentProcessor.setDefaultSource(profileInfo.id, sourceId).mapLeft { NotFoundError(it.description) } }
    }

    override fun listSources(subscriberId: String): Either<ApiError, List<SourceInfo>> {
        return getPaymentProfile(subscriberId)
                .fold(
                        { createAndStorePaymentProfile(subscriberId) },
                        { profileInfo -> Either.right(profileInfo) }
                )
                .flatMap { profileInfo -> paymentProcessor.getSavedSources(profileInfo.id).mapLeft { NotFoundError(it.description) } }

    }


}
