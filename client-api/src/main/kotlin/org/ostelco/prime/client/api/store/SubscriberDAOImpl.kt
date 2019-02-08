package org.ostelco.prime.client.api.store

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
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
import org.ostelco.prime.model.ActivePseudonyms
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.CustomerState
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.model.Subscription
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

    override fun getProfile(identity: Identity): Either<ApiError, Customer> {
        return try {
            storage.getCustomer(identity).mapLeft {
                NotFoundError("Failed to fetch profile.", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_PROFILE, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile with identity - $identity", e)
            Either.left(NotFoundError("Failed to fetch profile", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_PROFILE))
        }
    }

    override fun createProfile(
            identity: Identity,
            profile: Customer,
            referredBy: String?): Either<ApiError, Customer> {

        if (!SubscriberDAO.isValidProfile(profile)) {
            logger.error("Failed to create profile. Invalid profile.")
            return Either.left(BadRequestError("Incomplete profile description. Profile must contain name and email", ApiErrorCode.FAILED_TO_CREATE_PROFILE))
        }
        return try {
            // FIXME set subscriberId into profile
            storage.addCustomer(identity, profile, referredBy)
                    .mapLeft {
                        mapStorageErrorToApiError("Failed to create profile.", ApiErrorCode.FAILED_TO_CREATE_PROFILE, it)
                    }
                    .flatMap {
                        updateMetricsOnNewSubscriber()
                        getProfile(identity)
                    }
        } catch (e: Exception) {
            logger.error("Failed to create profile with identity - $identity", e)
            Either.left(BadGatewayError("Failed to create profile", ApiErrorCode.FAILED_TO_CREATE_PROFILE))
        }
    }

    override fun storeApplicationToken(customerId: String, applicationToken: ApplicationToken): Either<ApiError, ApplicationToken> {

        if (!SubscriberDAO.isValidApplicationToken(applicationToken)) {
            return Either.left(BadRequestError("Incomplete ApplicationToken", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN))
        }

        try {
            storage.addNotificationToken(customerId, applicationToken)
        } catch (e: Exception) {
            logger.error("Failed to store ApplicationToken for customerId $customerId", e)
            return Either.left(InsufficientStorageError("Failed to store ApplicationToken", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN))
        }
        return getNotificationToken(customerId, applicationToken.applicationID)
    }

    private fun getNotificationToken(customerId: String, applicationId: String): Either<ApiError, ApplicationToken> {
        try {
            return storage.getNotificationToken(customerId, applicationId)
                    ?.let { Either.right(it) }
                    ?: return Either.left(NotFoundError("Failed to get ApplicationToken", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN))
        } catch (e: Exception) {
            logger.error("Failed to get ApplicationToken for customerId $customerId", e)
            return Either.left(BadGatewayError("Failed to get ApplicationToken", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN))
        }
    }

    override fun updateProfile(identity: Identity, profile: Customer): Either<ApiError, Customer> {
        if (!SubscriberDAO.isValidProfile(profile)) {
            return Either.left(BadRequestError("Incomplete profile description", ApiErrorCode.FAILED_TO_UPDATE_PROFILE))
        }
        try {
            // FIXME set subscriberId into profile
            // FIXME only update editable fields
            storage.updateCustomer(identity, profile)
        } catch (e: Exception) {
            logger.error("Failed to update profile for customer with identity - $identity", e)
            return Either.left(BadGatewayError("Failed to update profile", ApiErrorCode.FAILED_TO_UPDATE_PROFILE))
        }

        return getProfile(identity)
    }

    override fun getSubscriptions(identity: Identity): Either<ApiError, Collection<Subscription>> {
        try {
            return storage.getSubscriptions(identity).mapLeft {
                NotFoundError("Failed to get subscriptions.", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to get subscriptions for customer with identity - $identity", e)
            return Either.left(BadGatewayError("Failed to get subscriptions", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS))
        }
    }

    override fun getBundles(identity: Identity): Either<ApiError, Collection<Bundle>> {
        return try {
            storage.getBundles(identity).mapLeft {
                NotFoundError("Failed to get bundles. ${it.message}", ApiErrorCode.FAILED_TO_FETCH_BUNDLES)
            }
        } catch (e: Exception) {
            logger.error("Failed to get bundles for customer with identity - $identity", e)
            Either.left(NotFoundError("Failed to get bundles", ApiErrorCode.FAILED_TO_FETCH_BUNDLES))
        }
    }

    override fun getActivePseudonymForSubscriber(identity: Identity): Either<ApiError, ActivePseudonyms> {
        return storage.getCustomerId(identity)
                .mapLeft { error -> mapStorageErrorToApiError(error.message, ApiErrorCode.FAILED_TO_FETCH_CUSTOMER_ID, error) }
                .flatMap { customerId ->
                    try {
                        pseudonymizer.getActivePseudonymsForSubscriberId(customerId).right()
                    } catch (e: Exception) {
                        NotFoundError("Failed to get pseudonym for user.", ApiErrorCode.FAILED_TO_FETCH_PSEUDONYM_FOR_SUBSCRIBER).left()
                    }
                }
    }

    override fun getPurchaseHistory(identity: Identity): Either<ApiError, Collection<PurchaseRecord>> {
        return try {
            return storage.getPurchaseRecords(identity).bimap(
                    { NotFoundError("Failed to get purchase history.", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_HISTORY, it) },
                    { it.toList() })
        } catch (e: Exception) {
            logger.error("Failed to get purchase history for customer with identity - $identity", e)
            Either.left(BadGatewayError("Failed to get purchase history", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_HISTORY))
        }
    }

    override fun getProducts(identity: Identity): Either<ApiError, Collection<Product>> {
        return try {
            storage.getProducts(identity).bimap(
                    { NotFoundError("Failed to fetch products", ApiErrorCode.FAILED_TO_FETCH_PRODUCT_LIST, it) },
                    { products -> products.values })
        } catch (e: Exception) {
            logger.error("Failed to get Products for customer with identity - $identity", e)
            Either.left(BadGatewayError("Failed to get Products", ApiErrorCode.FAILED_TO_FETCH_PRODUCT_LIST))
        }

    }

    override fun getProduct(identity: Identity, sku: String): Either<ApiError, Product> {
        return storage.getProduct(identity, sku)
                .fold({ Either.left(NotFoundError("Failed to get products for sku $sku", ApiErrorCode.FAILED_TO_FETCH_PRODUCT_INFORMATION)) },
                        { Either.right(it) })
    }

    override fun purchaseProduct(
            identity: Identity,
            sku: String,
            sourceId: String?,
            saveCard: Boolean): Either<ApiError, ProductInfo> =
            storage.purchaseProduct(
                    identity,
                    sku,
                    sourceId,
                    saveCard).mapLeft { mapPaymentErrorToApiError("Failed to purchase product. ", ApiErrorCode.FAILED_TO_PURCHASE_PRODUCT, it) }

    override fun getReferrals(identity: Identity): Either<ApiError, Collection<Person>> {
        return try {
            storage.getReferrals(identity).bimap(
                    { NotFoundError("Failed to get referral list.", ApiErrorCode.FAILED_TO_FETCH_REFERRALS, it) },
                    { list -> list.map { Person(it) } })
        } catch (e: Exception) {
            logger.error("Failed to get referral list for customer with identity - $identity", e)
            Either.left(BadGatewayError("Failed to get referral list", ApiErrorCode.FAILED_TO_FETCH_REFERRALS))
        }
    }

    override fun getReferredBy(identity: Identity): Either<ApiError, Person> {
        return try {
            storage.getReferredBy(identity).bimap(
                    { NotFoundError("Failed to get referred-by.", ApiErrorCode.FAILED_TO_FETCH_REFERRED_BY_LIST, it) },
                    { Person(name = it) })
        } catch (e: Exception) {
            logger.error("Failed to get referred-by for customer with identity - $identity", e)
            Either.left(BadGatewayError("Failed to get referred-by", ApiErrorCode.FAILED_TO_FETCH_REFERRED_BY_LIST))
        }
    }

    override fun getConsents(identity: Identity): Either<ApiError, Collection<Consent>> {
        consentMap.putIfAbsent(identity.id, ConcurrentHashMap())
        consentMap[identity.id]?.putIfAbsent("privacy", false)
        return Either.right(listOf(Consent(
                consentId = "privacy",
                description = "Grant permission to process personal data",
                accepted = consentMap[identity.id]?.get("privacy") ?: false)))
    }

    override fun acceptConsent(identity: Identity, consentId: String): Either<ApiError, Consent> {
        consentMap.putIfAbsent(identity.id, ConcurrentHashMap())
        consentMap[identity.id]?.put(consentId, true)
        return Either.right(Consent(consentId, "Grant permission to process personal data", true))
    }

    override fun rejectConsent(identity: Identity, consentId: String): Either<ApiError, Consent> {
        consentMap.putIfAbsent(identity.id, ConcurrentHashMap())
        consentMap[identity.id]?.put(consentId, false)
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

    override fun reportAnalytics(identity: Identity, events: String): Either<ApiError, Unit> = Either.right(Unit)

    override fun createSource(identity: Identity, sourceId: String): Either<ApiError, SourceInfo> {
        return storage.getCustomerId(identity)
                .mapLeft { error -> mapStorageErrorToApiError(error.message, ApiErrorCode.FAILED_TO_FETCH_CUSTOMER_ID, error) }
                .flatMap { customerId ->
                    paymentProcessor.getPaymentProfile(customerId = customerId)
                            .fold(
                                    {
                                        paymentProcessor.createPaymentProfile(customerId = customerId)
                                                .mapLeft { error -> mapPaymentErrorToApiError(error.description, ApiErrorCode.FAILED_TO_STORE_PAYMENT_SOURCE, error) }
                                    },
                                    { profileInfo -> Either.right(profileInfo) }
                            )
                }.flatMap { profileInfo ->
                    paymentProcessor.addSource(profileInfo.id, sourceId)
                            .mapLeft { mapPaymentErrorToApiError("Failed to store payment source", ApiErrorCode.FAILED_TO_STORE_PAYMENT_SOURCE, it) }
                }
    }

    override fun setDefaultSource(identity: Identity, sourceId: String): Either<ApiError, SourceInfo> {
        return storage.getCustomerId(identity)
                .mapLeft { error -> mapStorageErrorToApiError(error.message, ApiErrorCode.FAILED_TO_FETCH_CUSTOMER_ID, error) }
                .flatMap { customerId ->
                    paymentProcessor.getPaymentProfile(customerId = customerId)
                            .fold(
                                    {
                                        paymentProcessor.createPaymentProfile(customerId = customerId)
                                                .mapLeft { error -> mapPaymentErrorToApiError(error.description, ApiErrorCode.FAILED_TO_SET_DEFAULT_PAYMENT_SOURCE, error) }
                                    },
                                    { profileInfo -> Either.right(profileInfo) }
                            )
                }
                .flatMap { profileInfo ->
                    paymentProcessor.setDefaultSource(profileInfo.id, sourceId)
                            .mapLeft { mapPaymentErrorToApiError("Failed to set default payment source", ApiErrorCode.FAILED_TO_SET_DEFAULT_PAYMENT_SOURCE, it) }
                }
    }

    override fun listSources(identity: Identity): Either<ApiError, List<SourceDetailsInfo>> {
        return storage.getCustomerId(identity)
                .mapLeft { error -> mapStorageErrorToApiError(error.message, ApiErrorCode.FAILED_TO_FETCH_CUSTOMER_ID, error) }
                .flatMap { customerId ->
                    paymentProcessor.getPaymentProfile(customerId = customerId)
                            .fold(
                                    {
                                        paymentProcessor.createPaymentProfile(customerId = customerId)
                                                .mapLeft { error -> mapPaymentErrorToApiError(error.description, ApiErrorCode.FAILED_TO_FETCH_PAYMENT_SOURCES_LIST, error) }
                                    },
                                    { profileInfo -> Either.right(profileInfo) }
                            )
                }
                .flatMap { profileInfo ->
                    paymentProcessor.getSavedSources(profileInfo.id)
                            .mapLeft { mapPaymentErrorToApiError("Failed to list sources", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_SOURCES_LIST, it) }
                }
    }

    override fun removeSource(identity: Identity, sourceId: String): Either<ApiError, SourceInfo> {
        return storage.getCustomerId(identity)
                .mapLeft { error -> mapStorageErrorToApiError(error.message, ApiErrorCode.FAILED_TO_FETCH_CUSTOMER_ID, error) }
                .flatMap { customerId ->
                    paymentProcessor.getPaymentProfile(customerId = customerId)
                            .mapLeft { error -> mapPaymentErrorToApiError(error.description, ApiErrorCode.FAILED_TO_REMOVE_PAYMENT_SOURCE, error) }
                }
                .flatMap { profileInfo ->
                    paymentProcessor.removeSource(profileInfo.id, sourceId)
                            .mapLeft { mapPaymentErrorToApiError("Failed to remove payment source", ApiErrorCode.FAILED_TO_REMOVE_PAYMENT_SOURCE, it) }
                }
    }

    override fun getStripeEphemeralKey(identity: Identity, apiVersion: String): Either<ApiError, String> {
        return storage.getCustomerId(identity)
                .mapLeft { error -> mapStorageErrorToApiError(error.message, ApiErrorCode.FAILED_TO_FETCH_CUSTOMER_ID, error) }
                .flatMap { customerId ->
                    paymentProcessor.getStripeEphemeralKey(customerId = customerId, apiVersion = apiVersion)
                            .mapLeft { error -> mapPaymentErrorToApiError(error.description, ApiErrorCode.FAILED_TO_GENERATE_STRIPE_EPHEMERAL_KEY, error) }
                }
    }

    override fun newEKYCScanId(identity: Identity, countryCode: String): Either<ApiError, ScanInformation> {
        return storage.newEKYCScanId(identity, countryCode)
                .mapLeft { mapStorageErrorToApiError("Failed to create new scanId", ApiErrorCode.FAILED_TO_CREATE_SCANID, it) }
    }

    override fun getCountryCodeForScan(scanId: String): Either<ApiError, String> {
        return storage.getCountryCodeForScan(scanId)
                .mapLeft { mapStorageErrorToApiError("Failed to get country code of the scanId", ApiErrorCode.FAILED_TO_FETCH_SCAN_INFORMATION, it) }
    }

    override fun getScanInformation(identity: Identity, scanId: String): Either<ApiError, ScanInformation> {
        return storage.getScanInformation(identity, scanId)
                .mapLeft { mapStorageErrorToApiError("Failed to fetch scan information", ApiErrorCode.FAILED_TO_FETCH_SCAN_INFORMATION, it) }
    }

    override fun getSubscriberState(identity: Identity): Either<ApiError, CustomerState> {
        return storage.getCustomerState(identity)
                .mapLeft { mapStorageErrorToApiError("Failed to fetch new subscriber state", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIBER_STATE, it) }
    }
}
