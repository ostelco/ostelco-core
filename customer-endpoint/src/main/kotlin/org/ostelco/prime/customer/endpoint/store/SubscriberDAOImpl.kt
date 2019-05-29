package org.ostelco.prime.customer.endpoint.store

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.ApiErrorMapper.mapPaymentErrorToApiError
import org.ostelco.prime.apierror.ApiErrorMapper.mapStorageErrorToApiError
import org.ostelco.prime.apierror.BadRequestError
import org.ostelco.prime.apierror.InternalServerError
import org.ostelco.prime.apierror.NotFoundError
import org.ostelco.prime.customer.endpoint.metrics.updateMetricsOnNewSubscriber
import org.ostelco.prime.customer.endpoint.model.Person
import org.ostelco.prime.ekyc.MyInfoKycService
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Context
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.MyInfoConfig
import org.ostelco.prime.model.Product
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.RegionDetails
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.model.SimProfile
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.module.getResource
import org.ostelco.prime.paymentprocessor.PaymentProcessor
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.PlanAlredyPurchasedError
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
                NotFoundError("Failed to fetch customer.", ApiErrorCode.FAILED_TO_FETCH_CUSTOMER, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch customer with identity - $identity", e)
            Either.left(NotFoundError("Failed to fetch customer", ApiErrorCode.FAILED_TO_FETCH_CUSTOMER))
        }
    }

    override fun createCustomer(
            identity: Identity,
            customer: Customer,
            referredBy: String?): Either<ApiError, Customer> {

        return try {
            storage.addCustomer(identity, customer, referredBy)
                    .mapLeft {
                        mapStorageErrorToApiError("Failed to create customer.", ApiErrorCode.FAILED_TO_CREATE_CUSTOMER, it)
                    }
                    .flatMap {
                        updateMetricsOnNewSubscriber()
                        getCustomer(identity)
                    }
        } catch (e: Exception) {
            logger.error("Failed to create customer with identity - $identity", e)
            Either.left(InternalServerError("Failed to create customer", ApiErrorCode.FAILED_TO_CREATE_CUSTOMER))
        }
    }

    override fun updateCustomer(identity: Identity, nickname: String?, contactEmail: String?): Either<ApiError, Customer> {
        try {
            storage.updateCustomer(identity = identity, nickname = nickname, contactEmail = contactEmail)
        } catch (e: Exception) {
            logger.error("Failed to update customer with identity - $identity", e)
            return Either.left(InternalServerError("Failed to update customer", ApiErrorCode.FAILED_TO_UPDATE_CUSTOMER))
        }

        return getCustomer(identity)
    }

    override fun removeCustomer(identity: Identity): Either<ApiError, Unit> {
        return try {
            storage.removeCustomer(identity).mapLeft {
                NotFoundError("Failed to remove customer.", ApiErrorCode.FAILED_TO_REMOVE_CUSTOMER, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch customer with identity - $identity", e)
            Either.left(NotFoundError("Failed to remove customer", ApiErrorCode.FAILED_TO_REMOVE_CUSTOMER))
        }
    }

    //
    // Context
    //

    override fun getContext(identity: Identity): Either<ApiError, Context> {
        return try {
            storage.getCustomer(identity)
                    .mapLeft {
                        NotFoundError("Failed to fetch customer.", ApiErrorCode.FAILED_TO_FETCH_CONTEXT, it)
                    }
                    .map { customer ->
                        storage.getAllRegionDetails(identity = identity)
                                .fold(
                                        { Context(customer = customer) },
                                        { regionDetailsCollection -> Context(customer = customer, regions = regionDetailsCollection) })
                    }
        } catch (e: Exception) {
            logger.error("Failed to fetch context for customer with identity - $identity", e)
            Either.left(NotFoundError("Failed to fetch context", ApiErrorCode.FAILED_TO_FETCH_CONTEXT))
        }
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
            Either.left(InternalServerError("Failed to get regions", ApiErrorCode.FAILED_TO_FETCH_REGIONS))
        }
    }

    override fun getRegion(identity: Identity, regionCode: String): Either<ApiError, RegionDetails> {
        return try {
            storage.getRegionDetails(identity, regionCode).mapLeft {
                NotFoundError("Failed to get regions.", ApiErrorCode.FAILED_TO_FETCH_REGIONS, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to get regions for customer with identity - $identity", e)
            Either.left(InternalServerError("Failed to get regions", ApiErrorCode.FAILED_TO_FETCH_REGIONS))
        }
    }

    //
    // Subscriptions
    //

    override fun getSubscriptions(identity: Identity, regionCode: String): Either<ApiError, Collection<Subscription>> {
        return try {
            storage.getSubscriptions(identity, regionCode).mapLeft {
                NotFoundError("Failed to get subscriptions.", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to get subscriptions for customer with identity - $identity", e)
            Either.left(InternalServerError("Failed to get subscriptions", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS))
        }
    }

    //
    // SIM Profile
    //

    override fun getSimProfiles(identity: Identity, regionCode: String): Either<ApiError, Collection<SimProfile>> {
        return try {
            storage.getSimProfiles(identity, regionCode).mapLeft {
                NotFoundError("Failed to fetch SIM profiles.", ApiErrorCode.FAILED_TO_FETCH_SIM_PROFILES, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch SIM profiles for customer with identity - $identity", e)
            Either.left(InternalServerError("Failed to fetch SIM profiles", ApiErrorCode.FAILED_TO_FETCH_SIM_PROFILES))
        }
    }

    override fun provisionSimProfile(identity: Identity, regionCode: String, profileType: String?): Either<ApiError, SimProfile> {
        return try {
            storage.provisionSimProfile(identity, regionCode, profileType).mapLeft {
                NotFoundError("Failed to provision SIM profile.", ApiErrorCode.FAILED_TO_PROVISION_SIM_PROFILE, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to provision SIM profile for customer with identity - $identity", e)
            Either.left(InternalServerError("Failed to provision SIM profile", ApiErrorCode.FAILED_TO_PROVISION_SIM_PROFILE))
        }
    }

    override fun updateSimProfile(identity: Identity, regionCode: String, iccId: String, alias: String): Either<ApiError, SimProfile> {
        return try {
            storage.updateSimProfile(identity, regionCode, iccId, alias).mapLeft {
                NotFoundError("Failed to update SIM profile.", ApiErrorCode.FAILED_TO_UPDATE_SIM_PROFILE, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to update SIM profile for customer with identity - $identity", e)
            Either.left(InternalServerError("Failed to update SIM profile", ApiErrorCode.FAILED_TO_UPDATE_SIM_PROFILE))
        }
    }

    override fun sendEmailWithEsimActivationQrCode(identity: Identity, regionCode: String, iccId: String): Either<ApiError, SimProfile> {
        return try {
            storage.sendEmailWithActivationQrCode(identity, regionCode, iccId).mapLeft {
                NotFoundError("Failed to send email with Activation QR code.", ApiErrorCode.FAILED_TO_SEND_EMAIL_WITH_ESIM_ACTIVATION_QR_CODE, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to send email with Activation QR code for customer with identity - $identity", e)
            Either.left(InternalServerError("Failed to send email with Activation QR code", ApiErrorCode.FAILED_TO_SEND_EMAIL_WITH_ESIM_ACTIVATION_QR_CODE))
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
            Either.left(InternalServerError("Failed to get purchase history", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_HISTORY))
        }
    }

    override fun getProducts(identity: Identity): Either<ApiError, Collection<Product>> {
        return try {
            storage.getProducts(identity).bimap(
                    { NotFoundError("Failed to fetch products", ApiErrorCode.FAILED_TO_FETCH_PRODUCT_LIST, it) },
                    { products -> products.values })
        } catch (e: Exception) {
            logger.error("Failed to get Products for customer with identity - $identity", e)
            Either.left(InternalServerError("Failed to get Products", ApiErrorCode.FAILED_TO_FETCH_PRODUCT_LIST))
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
                    saveCard).mapLeft {
                when (it) {
                    is PlanAlredyPurchasedError -> mapPaymentErrorToApiError("Already subscribed to plan. ",
                            ApiErrorCode.ALREADY_SUBSCRIBED_TO_PLAN,
                            it)
                    else -> mapPaymentErrorToApiError("Failed to purchase product. ",
                            ApiErrorCode.FAILED_TO_PURCHASE_PRODUCT,
                            it)
                }
            }

    //
    // Payment
    //

    override fun createSource(identity: Identity, sourceId: String): Either<ApiError, SourceInfo> {
        return storage.getCustomer(identity)
                .mapLeft { error -> mapStorageErrorToApiError(error.message, ApiErrorCode.FAILED_TO_FETCH_CUSTOMER, error) }
                .flatMap { customer ->
                    paymentProcessor.getPaymentProfile(customerId = customer.id)
                            .fold(
                                    {
                                        paymentProcessor.createPaymentProfile(customerId = customer.id, email = customer.contactEmail)
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
        return storage.getCustomer(identity)
                .mapLeft { error -> mapStorageErrorToApiError(error.message, ApiErrorCode.FAILED_TO_FETCH_CUSTOMER_ID, error) }
                .flatMap { customer ->
                    paymentProcessor.getPaymentProfile(customerId = customer.id)
                            .fold(
                                    {
                                        paymentProcessor.createPaymentProfile(customerId = customer.id, email = customer.contactEmail)
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
        return storage.getCustomer(identity)
                .mapLeft { error -> mapStorageErrorToApiError(error.message, ApiErrorCode.FAILED_TO_FETCH_CUSTOMER_ID, error) }
                .flatMap { customer ->
                    paymentProcessor.getPaymentProfile(customerId = customer.id)
                            .fold(
                                    {
                                        paymentProcessor.createPaymentProfile(customerId = customer.id, email = customer.contactEmail)
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
        return storage.getCustomer(identity)
                .mapLeft { error -> mapStorageErrorToApiError(error.message, ApiErrorCode.FAILED_TO_FETCH_CUSTOMER_ID, error) }
                .flatMap { customer ->
                    paymentProcessor.getStripeEphemeralKey(customerId = customer.id, email = customer.contactEmail, apiVersion = apiVersion)
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
            Either.left(InternalServerError("Failed to get referral list", ApiErrorCode.FAILED_TO_FETCH_REFERRALS))
        }
    }

    override fun getReferredBy(identity: Identity): Either<ApiError, Person> {
        return try {
            storage.getReferredBy(identity).bimap(
                    { NotFoundError("Failed to get referred-by.", ApiErrorCode.FAILED_TO_FETCH_REFERRED_BY_LIST, it) },
                    { Person(name = it) })
        } catch (e: Exception) {
            logger.error("Failed to get referred-by for customer with identity - $identity", e)
            Either.left(InternalServerError("Failed to get referred-by", ApiErrorCode.FAILED_TO_FETCH_REFERRED_BY_LIST))
        }
    }

    //
    // eKYC
    //

    override fun createNewJumioKycScanId(identity: Identity, regionCode: String): Either<ApiError, ScanInformation> {
        return storage.createNewJumioKycScanId(identity, regionCode)
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
        return storage.getCustomerMyInfoData(identity, authorisationCode)
                .mapLeft { mapStorageErrorToApiError("Failed to fetch Customer Data from MyInfo", ApiErrorCode.FAILED_TO_FETCH_CUSTOMER_MYINFO_DATA, it) }
    }

    private val myInfoKycService by lazy { getResource<MyInfoKycService>() }

    override fun getMyInfoConfig(): Either<ApiError, MyInfoConfig> = myInfoKycService.getConfig().right()

    override fun checkNricFinIdUsingDave(identity: Identity, nricFinId: String): Either<ApiError, Unit> {
        return storage.checkNricFinIdUsingDave(identity, nricFinId)
                .mapLeft { mapStorageErrorToApiError("Invalid NRIC/FIN ID", ApiErrorCode.INVALID_NRIC_FIN_ID, it) }
    }

    override fun saveAddressAndPhoneNumber(identity: Identity, address: String, phoneNumber: String): Either<ApiError, Unit> {
        return storage.saveAddressAndPhoneNumber(identity = identity, address = address, phoneNumber = phoneNumber)
                .mapLeft { mapStorageErrorToApiError("Failed to save address and phone number", ApiErrorCode.FAILED_TO_SAVE_ADDRESS_AND_PHONE_NUMBER, it) }
    }

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
            return Either.left(InternalServerError("Failed to store ApplicationToken", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN))
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
            return Either.left(InternalServerError("Failed to get ApplicationToken", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN))
        }
    }
}
