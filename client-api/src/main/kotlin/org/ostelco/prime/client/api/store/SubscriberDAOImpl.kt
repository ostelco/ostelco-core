package org.ostelco.prime.client.api.store

import arrow.core.Either
import arrow.core.flatMap
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.ApiErrorMapper.mapPaymentErrorToApiError
import org.ostelco.prime.apierror.ApiErrorMapper.mapStorageErrorToApiError
import org.ostelco.prime.apierror.BadGatewayError
import org.ostelco.prime.apierror.BadRequestError
import org.ostelco.prime.apierror.InsufficientStorageError
import org.ostelco.prime.apierror.NotFoundError
import org.ostelco.prime.client.api.metrics.updateMetricsOnNewSubscriber
import org.ostelco.prime.client.api.model.Consent
import org.ostelco.prime.client.api.model.Person
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.*
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.paymentprocessor.core.ProfileInfo
import org.ostelco.prime.paymentprocessor.core.SourceDetailsInfo
import org.ostelco.prime.paymentprocessor.core.SourceInfo
import org.ostelco.prime.pseudonymizer.PseudonymizerService
import org.ostelco.prime.storage.ClientDataSource
import org.ostelco.prime.storage.StoreError
import java.util.concurrent.ConcurrentHashMap

/**
 *
 */
class SubscriberDAOImpl : SubscriberDAO {

    private val logger by getLogger()

    private val storage by lazy { getResource<ClientDataSource>() }
    private val paymentProcessor by lazy { getResource<PaymentProcessor>() }
    private val pseudonymizer by lazy { getResource<PseudonymizerService>() }

    /* Table for 'profiles'. */
    private val consentMap = ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>>()

    override fun getProfile(subscriberId: String): Either<ApiError, Subscriber> {
        return try {
            storage.getSubscriber(subscriberId).mapLeft {
                NotFoundError("Failed to fetch profile.", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_PROFILE, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile for subscriberId $subscriberId", e)
            Either.left(NotFoundError("Failed to fetch profile", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_PROFILE))
        }
    }

    override fun createProfile(subscriberId: String, profile: Subscriber, referredBy: String?): Either<ApiError, Subscriber> {
        if (!SubscriberDAO.isValidProfile(profile)) {
            logger.error("Failed to create profile. Invalid profile.")
            return Either.left(BadRequestError("Incomplete profile description. Profile must contain name and email", ApiErrorCode.FAILED_TO_CREATE_PROFILE))
        }
        return try {
            // FIXME set subscriberId into profile
            storage.addSubscriber(profile, referredBy)
                    .mapLeft {
                        mapStorageErrorToApiError("Failed to create profile.", ApiErrorCode.FAILED_TO_CREATE_PROFILE, it)
                    }
                    .flatMap {
                        updateMetricsOnNewSubscriber()
                        getProfile(subscriberId)
                    }
        } catch (e: Exception) {
            logger.error("Failed to create profile for subscriberId $subscriberId", e)
            Either.left(BadGatewayError("Failed to create profile", ApiErrorCode.FAILED_TO_CREATE_PROFILE))
        }
    }

    override fun storeApplicationToken(msisdn: String, applicationToken: ApplicationToken): Either<ApiError, ApplicationToken> {

        if (!SubscriberDAO.isValidApplicationToken(applicationToken)) {
            return Either.left(BadRequestError("Incomplete ApplicationToken", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN))
        }

        try {
            storage.addNotificationToken(msisdn, applicationToken)
        } catch (e: Exception) {
            logger.error("Failed to store ApplicationToken for msisdn $msisdn", e)
            return Either.left(InsufficientStorageError("Failed to store ApplicationToken", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN))
        }
        return getNotificationToken(msisdn, applicationToken.applicationID)
    }

    private fun getNotificationToken(msisdn: String, applicationId: String): Either<ApiError, ApplicationToken> {
        try {
            return storage.getNotificationToken(msisdn, applicationId)
                    ?.let { Either.right(it) }
                    ?: return Either.left(NotFoundError("Failed to get ApplicationToken", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN))
        } catch (e: Exception) {
            logger.error("Failed to get ApplicationToken for msisdn $msisdn", e)
            return Either.left(BadGatewayError("Failed to get ApplicationToken", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN))
        }
    }

    override fun updateProfile(subscriberId: String, profile: Subscriber): Either<ApiError, Subscriber> {
        if (!SubscriberDAO.isValidProfile(profile)) {
            return Either.left(BadRequestError("Incomplete profile description", ApiErrorCode.FAILED_TO_UPDATE_PROFILE))
        }
        try {
            // FIXME set subscriberId into profile
            // FIXME only update editable fields
            storage.updateSubscriber(profile)
        } catch (e: Exception) {
            logger.error("Failed to update profile for subscriberId $subscriberId", e)
            return Either.left(BadGatewayError("Failed to update profile", ApiErrorCode.FAILED_TO_UPDATE_PROFILE))
        }

        return getProfile(subscriberId)
    }

    override fun getSubscriptions(subscriberId: String): Either<ApiError, Collection<Subscription>> {
        try {
            return storage.getSubscriptions(subscriberId).mapLeft {
                NotFoundError("Failed to get subscriptions.", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to get subscriptions for subscriberId $subscriberId", e)
            return Either.left(BadGatewayError("Failed to get subscriptions", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS))
        }
    }

    override fun getBundles(subscriberId: String): Either<ApiError, Collection<Bundle>> {
        return try {
            storage.getBundles(subscriberId).mapLeft {
                NotFoundError("Failed to get bundles. ${it.message}", ApiErrorCode.FAILED_TO_FETCH_BUNDLES)
            }
        } catch (e: Exception) {
            logger.error("Failed to get bundles for subscriberId $subscriberId", e)
            Either.left(NotFoundError("Failed to get bundles", ApiErrorCode.FAILED_TO_FETCH_BUNDLES))
        }
    }

    override fun getActivePseudonymForSubscriber(subscriberId: String): Either<ApiError, ActivePseudonyms> {
        return try {
            Either.right(pseudonymizer.getActivePseudonymsForSubscriberId(subscriberId))
        } catch (e: Exception) {
            Either.left(NotFoundError("Failed to get pseudonym for user.", ApiErrorCode.FAILED_TO_FETCH_PSEUDONYM_FOR_SUBSCRIBER))
        }
    }

    override fun getPurchaseHistory(subscriberId: String): Either<ApiError, Collection<PurchaseRecord>> {
        return try {
            return storage.getPurchaseRecords(subscriberId).bimap(
                    { NotFoundError("Failed to get purchase history.", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_HISTORY, it) },
                    { it.toList() })
        } catch (e: Exception) {
            logger.error("Failed to get purchase history for subscriberId $subscriberId", e)
            Either.left(BadGatewayError("Failed to get purchase history", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_HISTORY))
        }
    }

    override fun getMsisdn(subscriberId: String): Either<ApiError, String> {
        return try {
            storage.getMsisdn(subscriberId).mapLeft {
                NotFoundError("Did not find msisdn for this subscription.", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN, it)
            }
        } catch (e: Exception) {
            logger.error("Did not find msisdn for subscriberId $subscriberId", e)
            Either.left(BadGatewayError("Did not find subscription", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN))
        }
    }

    override fun getProducts(subscriberId: String): Either<ApiError, Collection<Product>> {
        return try {
            storage.getProducts(subscriberId).bimap(
                    { NotFoundError("Failed to fetch products", ApiErrorCode.FAILED_TO_FETCH_PRODUCT_LIST, it) },
                    { products -> products.values })
        } catch (e: Exception) {
            logger.error("Failed to get Products for subscriberId $subscriberId", e)
            Either.left(BadGatewayError("Failed to get Products", ApiErrorCode.FAILED_TO_FETCH_PRODUCT_LIST))
        }

    }

    override fun getProduct(subscriptionId: String, sku: String): Either<ApiError, Product> {
        return storage.getProduct(subscriptionId, sku)
                .fold({ Either.left(NotFoundError("Failed to get products for sku $sku", ApiErrorCode.FAILED_TO_FETCH_PRODUCT_INFORMATION)) },
                        { Either.right(it) })
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
                    saveCard).mapLeft { mapPaymentErrorToApiError("Failed to purchase product. ", ApiErrorCode.FAILED_TO_PURCHASE_PRODUCT, it) }

    override fun getReferrals(subscriberId: String): Either<ApiError, Collection<Person>> {
        return try {
            storage.getReferrals(subscriberId).bimap(
                    { NotFoundError("Failed to get referral list.", ApiErrorCode.FAILED_TO_FETCH_REFERRALS, it) },
                    { list -> list.map { Person(it) } })
        } catch (e: Exception) {
            logger.error("Failed to get referral list for subscriberId $subscriberId", e)
            Either.left(BadGatewayError("Failed to get referral list", ApiErrorCode.FAILED_TO_FETCH_REFERRALS))
        }
    }

    override fun getReferredBy(subscriberId: String): Either<ApiError, Person> {
        return try {
            storage.getReferredBy(subscriberId).bimap(
                    { NotFoundError("Failed to get referred-by.", ApiErrorCode.FAILED_TO_FETCH_REFERRED_BY_LIST, it) },
                    { Person(name = it) })
        } catch (e: Exception) {
            logger.error("Failed to get referred-by for subscriberId $subscriberId", e)
            Either.left(BadGatewayError("Failed to get referred-by", ApiErrorCode.FAILED_TO_FETCH_REFERRED_BY_LIST))
        }
    }

    override fun getConsents(subscriberId: String): Either<ApiError, Collection<Consent>> {
        consentMap.putIfAbsent(subscriberId, ConcurrentHashMap())
        consentMap[subscriberId]?.putIfAbsent("privacy", false)
        return Either.right(listOf(Consent(
                consentId = "privacy",
                description = "Grant permission to process personal data",
                accepted = consentMap[subscriberId]?.get("privacy") ?: false)))
    }

    override fun acceptConsent(subscriberId: String, consentId: String): Either<ApiError, Consent> {
        consentMap.putIfAbsent(subscriberId, ConcurrentHashMap())
        consentMap[subscriberId]?.put(consentId, true)
        return Either.right(Consent(consentId, "Grant permission to process personal data", true))
    }

    override fun rejectConsent(subscriberId: String, consentId: String): Either<ApiError, Consent> {
        consentMap.putIfAbsent(subscriberId, ConcurrentHashMap())
        consentMap[subscriberId]?.put(consentId, false)
        return Either.right(Consent(consentId, "Grant permission to process personal data", false))
    }

    private fun getPaymentProfile(name: String): Either<StoreError, ProfileInfo> =
            storage.getPaymentId(name)
                    ?.let { profileInfoId -> Either.right(ProfileInfo(profileInfoId)) }
                    ?: Either.left(org.ostelco.prime.storage.NotFoundError("Failed to fetch payment customer ID", name))

    private fun setPaymentProfile(name: String, profileInfo: ProfileInfo): Either<StoreError, Unit> =
            Either.cond(
                    test = storage.createPaymentId(name, profileInfo.id),
                    ifTrue = { Unit },
                    ifFalse = { org.ostelco.prime.storage.NotCreatedError("Failed to store payment customer ID") })

    override fun reportAnalytics(subscriberId: String, events: String): Either<ApiError, Unit> = Either.right(Unit)

    override fun createSource(subscriberId: String, sourceId: String): Either<ApiError, SourceInfo> {
        return paymentProcessor.getPaymentProfile(subscriberId)
                .fold(
                        {
                            paymentProcessor.createPaymentProfile(subscriberId)
                                    .mapLeft { error -> mapPaymentErrorToApiError(error.description, ApiErrorCode.FAILED_TO_STORE_PAYMENT_SOURCE, error) }
                        },
                        { profileInfo -> Either.right(profileInfo) }
                )
                .flatMap { profileInfo ->
                    paymentProcessor.addSource(profileInfo.id, sourceId)
                            .mapLeft { mapPaymentErrorToApiError("Failed to store payment source", ApiErrorCode.FAILED_TO_STORE_PAYMENT_SOURCE, it) }
                }
    }

    override fun setDefaultSource(subscriberId: String, sourceId: String): Either<ApiError, SourceInfo> {
        return paymentProcessor.getPaymentProfile(subscriberId)
                .fold(
                        {
                            paymentProcessor.createPaymentProfile(subscriberId)
                                    .mapLeft { error -> mapPaymentErrorToApiError(error.description, ApiErrorCode.FAILED_TO_SET_DEFAULT_PAYMENT_SOURCE, error) }
                        },
                        { profileInfo -> Either.right(profileInfo) }
                )
                .flatMap { profileInfo ->
                    paymentProcessor.setDefaultSource(profileInfo.id, sourceId)
                            .mapLeft { mapPaymentErrorToApiError("Failed to set default payment source", ApiErrorCode.FAILED_TO_SET_DEFAULT_PAYMENT_SOURCE, it) }
                }
    }

    override fun listSources(subscriberId: String): Either<ApiError, List<SourceDetailsInfo>> {
        return paymentProcessor.getPaymentProfile(subscriberId)
                .fold(
                        {
                            paymentProcessor.createPaymentProfile(subscriberId)
                                    .mapLeft { error -> mapPaymentErrorToApiError(error.description, ApiErrorCode.FAILED_TO_FETCH_PAYMENT_SOURCES_LIST, error) }
                        },
                        { profileInfo -> Either.right(profileInfo) }
                )
                .flatMap { profileInfo ->
                    paymentProcessor.getSavedSources(profileInfo.id)
                            .mapLeft { mapPaymentErrorToApiError("Failed to list sources", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_SOURCES_LIST, it) }
                }
    }

    override fun removeSource(subscriberId: String, sourceId: String): Either<ApiError, SourceInfo> {
        return paymentProcessor.getPaymentProfile(subscriberId)
                .mapLeft { error -> mapPaymentErrorToApiError(error.description, ApiErrorCode.FAILED_TO_REMOVE_PAYMENT_SOURCE, error) }
                .flatMap { profileInfo ->
                    paymentProcessor.removeSource(profileInfo.id, sourceId)
                            .mapLeft { mapPaymentErrorToApiError("Failed to remove payment source", ApiErrorCode.FAILED_TO_REMOVE_PAYMENT_SOURCE, it) }
                }
    }

    override fun getStripeEphemeralKey(subscriberId: String, apiVersion: String): Either<ApiError, String> {
        return paymentProcessor.getStripeEphemeralKey(userEmail = subscriberId, apiVersion = apiVersion)
                .mapLeft { error -> mapPaymentErrorToApiError(error.description, ApiErrorCode.FAILED_TO_GENERATE_STRIPE_EPHEMERAL_KEY, error) }
    }

    override fun newEKYCScanId(subscriberId: String): Either<ApiError, ScanInformation> {
        return storage.newEKYCScanId(subscriberId)
                .mapLeft { mapStorageErrorToApiError("Failed to create new scanId", ApiErrorCode.FAILED_TO_CREATE_SCANID, it) }
    }

    override fun getScanInformation(subscriberId: String, scanId: String): Either<ApiError, ScanInformation> {
        return storage.getScanInformation(subscriberId, scanId)
                .mapLeft { mapStorageErrorToApiError("Failed to fetch scan information", ApiErrorCode.FAILED_TO_FETCH_SCAN_INFORMATION, it) }
    }

    override fun getSubscriberState(subscriberId: String): Either<ApiError, SubscriberState> {
        return storage.getSubscriberState(subscriberId)
                .mapLeft { mapStorageErrorToApiError("Failed to fetch new subscriber state", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIBER_STATE, it) }
    }
}
