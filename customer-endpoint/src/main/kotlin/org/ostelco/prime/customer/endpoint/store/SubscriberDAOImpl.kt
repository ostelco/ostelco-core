package org.ostelco.prime.customer.endpoint.store

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import org.ostelco.prime.apierror.*
import org.ostelco.prime.apierror.ApiErrorMapper.mapPaymentErrorToApiError
import org.ostelco.prime.apierror.ApiErrorMapper.mapStorageErrorToApiError
import org.ostelco.prime.customer.endpoint.metrics.updateMetricsOnNewSubscriber
import org.ostelco.prime.customer.endpoint.model.Person
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.*
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.paymentprocessor.core.SourceDetailsInfo
import org.ostelco.prime.paymentprocessor.core.SourceInfo
import org.ostelco.prime.storage.ClientDataSource

/**
 *
 */
class SubscriberDAOImpl : SubscriberDAO {

    private val logger by getLogger()

    private val storage by lazy { getResource<ClientDataSource>() }
    private val paymentProcessor by lazy { getResource<PaymentProcessor>() }

    //
    // Customer
    //

    override fun getCustomer(identity: Identity): Either<ApiError, Customer> {
        return try {
            storage.getCustomer(identity).mapLeft {
                NotFoundError("Failed to fetch profile.", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_PROFILE, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch customer with identity - $identity", e)
            Either.left(NotFoundError("Failed to fetch profile", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_PROFILE))
        }
    }

    override fun createCustomer(
            identity: Identity,
            profile: Customer,
            referredBy: String?): Either<ApiError, Customer> {

        if (!SubscriberDAO.isValidProfile(profile)) {
            logger.error("Failed to create customer. Invalid customer info.")
            return Either.left(BadRequestError("Incomplete profile description. Customer info must contain name and email", ApiErrorCode.FAILED_TO_CREATE_PROFILE))
        }
        return try {
            // FIXME set subscriberId into profile
            storage.addCustomer(identity, profile, referredBy)
                    .mapLeft {
                        mapStorageErrorToApiError("Failed to create customer.", ApiErrorCode.FAILED_TO_CREATE_PROFILE, it)
                    }
                    .flatMap {
                        updateMetricsOnNewSubscriber()
                        getCustomer(identity)
                    }
        } catch (e: Exception) {
            logger.error("Failed to create customer with identity - $identity", e)
            Either.left(BadGatewayError("Failed to create customer", ApiErrorCode.FAILED_TO_CREATE_PROFILE))
        }
    }

    override fun updateCustomer(identity: Identity, profile: Customer): Either<ApiError, Customer> {
        if (!SubscriberDAO.isValidProfile(profile)) {
            return Either.left(BadRequestError("Incomplete customer info description", ApiErrorCode.FAILED_TO_UPDATE_PROFILE))
        }
        try {
            // FIXME set subscriberId into profile
            // FIXME only update editable fields
            storage.updateCustomer(identity, profile)
        } catch (e: Exception) {
            logger.error("Failed to update customer with identity - $identity", e)
            return Either.left(BadGatewayError("Failed to update customer", ApiErrorCode.FAILED_TO_UPDATE_PROFILE))
        }

        return getCustomer(identity)
    }

    //
    // Regions
    //
    override fun getRegions(identity: Identity): Either<ApiError, Collection<RegionDetails>> {
        return try {
            storage.getAllRegionDetails(identity).mapLeft {
                NotFoundError("Failed to get regions.", ApiErrorCode.FAILED_TO_FETCH_REGIONS, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to get regions for customer with identity - $identity", e)
            Either.left(BadGatewayError("Failed to get regions", ApiErrorCode.FAILED_TO_FETCH_REGIONS))
        }
    }

    //
    // Subscriptions
    //

    override fun getSubscriptions(identity: Identity): Either<ApiError, Collection<Subscription>> {
        return try {
            storage.getSubscriptions(identity).mapLeft {
                NotFoundError("Failed to get subscriptions.", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to get subscriptions for customer with identity - $identity", e)
            Either.left(BadGatewayError("Failed to get subscriptions", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS))
        }
    }

    override fun createSubscription(identity: Identity): Either<ApiError, Subscription> {
        return try {
            storage.createSubscription(identity).mapLeft {
                NotFoundError("Failed to create subscription.", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to create subscription for customer with identity - $identity", e)
            Either.left(BadGatewayError("Failed to create subscription", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS))
        }
    }

    //
    // Bundle
    //

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

    //
    // Products
    //

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

    //
    // Payment
    //

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

    //
    // Referrals
    //

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

    //
    // eKYC
    //

    override fun createNewJumioScanId(identity: Identity, countryCode: String): Either<ApiError, ScanInformation> {
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

    override fun getCustomerMyInfoData(identity: Identity, authorisationCode: String): Either<ApiError, String> {
        return "{}".right()
    }

    override fun checkIdNumberUsingDave(identity: Identity) : Either<ApiError, Unit> = Unit.right()

    override fun saveProfile(identity: Identity) : Either<ApiError, Unit> = Unit.right()
    //
    // Token
    //

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
}
