package org.ostelco.prime.support.resources

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import io.dropwizard.auth.Auth
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.ApiErrorCode.FAILED_TO_FETCH_AUDIT_LOGS
import org.ostelco.prime.apierror.ApiErrorMapper
import org.ostelco.prime.apierror.InternalServerError
import org.ostelco.prime.apierror.NotFoundError
import org.ostelco.prime.apierror.responseBuilder
import org.ostelco.prime.appnotifier.AppNotifier
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Context
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.module.getResource
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.storage.AdminDataSource
import org.ostelco.prime.storage.AuditLogStore
import java.util.regex.Pattern
import javax.validation.constraints.NotNull
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Resource used to handle the profile related REST calls.
 */
@Path("/profiles")
class ProfilesResource {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Get the subscriber profile.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getProfile(@Auth token: AccessTokenPrincipal?,
                   @NotNull
                   @PathParam("id")
                   id: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                if (!isEmail(id)) {
                    logger.info("${token.name} Accessing profile for msisdn:$id")
                    getProfileForMsisdn(id)
                            .responseBuilder()
                } else {
                    logger.info("${token.name} Accessing profile for email:$id")
                    getProfile(contactEmail = id)
                            .responseBuilder()
                }
            }.build()

    /**
     * Get the subscriptions for this subscriber.
     */
    @GET
    @Path("{email}/subscriptions")
    @Produces(MediaType.APPLICATION_JSON)
    fun getSubscriptions(@Auth token: AccessTokenPrincipal?,
                         @NotNull
                         @PathParam("email")
                         email: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Accessing subscriptions for email:$email")
                getSubscriptions(contactEmail = email)
                        .responseBuilder()
            }.build()


    /**
     * Get all the eKYC scan information for this subscriber.
     */
    @GET
    @Path("{email}/scans")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllScanInformation(@Auth token: AccessTokenPrincipal?,
                              @NotNull
                              @PathParam("email")
                              email: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Accessing scan information for email:$email")
                getAllScanInformation(contactEmail = email)
                        .responseBuilder()
            }.build()

    private fun getAllScanInformation(contactEmail: String): Either<ApiError, Collection<ScanInformation>> {
        return try {
            storage.getIdentityForContactEmail(contactEmail = contactEmail).flatMap {identity: Identity ->
                storage.getAllScanInformation(identity = identity)
            }.mapLeft {
                NotFoundError("Failed to fetch scan information.", ApiErrorCode.FAILED_TO_FETCH_SCAN_INFORMATION, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch scan information for customer with contactEmail - $contactEmail", e)
            Either.left(NotFoundError("Failed to fetch scan information", ApiErrorCode.FAILED_TO_FETCH_SCAN_INFORMATION))
        }
    }

    // TODO: Reuse the one from SubscriberDAO
    private fun getProfile(contactEmail: String): Either<ApiError, Customer> {
        return try {
            storage.getIdentityForContactEmail(contactEmail = contactEmail).flatMap {identity: Identity ->
                storage.getCustomer(identity)
            }.mapLeft {
                NotFoundError("Failed to fetch profile.", ApiErrorCode.FAILED_TO_FETCH_CUSTOMER, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile for customer with contactEmail - $contactEmail", e)
            Either.left(NotFoundError("Failed to fetch profile", ApiErrorCode.FAILED_TO_FETCH_CUSTOMER))
        }
    }

    private fun isEmail(email: String): Boolean {
        val regex = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"
        val pattern = Pattern.compile(regex)
        return pattern.matcher(email).matches()
    }

    private fun getProfileForMsisdn(msisdn: String): Either<ApiError, Customer> {
        return try {
            storage.getCustomerForMsisdn(msisdn).mapLeft {
                NotFoundError("Failed to fetch profile.", ApiErrorCode.FAILED_TO_FETCH_CUSTOMER, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile for msisdn $msisdn", e)
            Either.left(NotFoundError("Failed to fetch profile", ApiErrorCode.FAILED_TO_FETCH_CUSTOMER))
        }
    }

    // TODO: Reuse the one from SubscriberDAO
    private fun getSubscriptions(contactEmail: String): Either<ApiError, Collection<Subscription>> {
        return try {
            storage.getIdentityForContactEmail(contactEmail = contactEmail).flatMap {identity: Identity ->
                storage.getSubscriptions(identity)
            }.mapLeft {
                NotFoundError("Failed to get subscriptions.", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to get subscriptions for customer with contactEmail - $contactEmail", e)
            InternalServerError("Failed to get subscriptions", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS).left()
        }
    }

    /**
     * Fetches and return all plans that a subscriber subscribes
     * to if any.
     */
    @GET
    @Path("{email}/plans")
    @Produces("application/json")
    fun getPlans(@PathParam("email") email: String): Response =
            storage.getIdentityForContactEmail(contactEmail = email).flatMap { identity: Identity ->
                storage.getPlans(identity = identity)
            }.mapLeft {
                ApiErrorMapper.mapStorageErrorToApiError("Failed to fetch plans",
                        ApiErrorCode.FAILED_TO_FETCH_PLANS_FOR_SUBSCRIBER,
                        it)
            }
                    .responseBuilder()
                    .build()

    /**
     * Attaches (subscribes) a subscriber to a plan.
     */
    @POST
    @Path("{email}/plans/{planId}")
    @Produces("application/json")
    fun attachPlan(@PathParam("email") email: String,
                   @PathParam("planId") planId: String,
                   @QueryParam("trial_end") trialEnd: Long): Response =
            storage.getIdentityForContactEmail(contactEmail = email).flatMap { identity: Identity ->
                storage.subscribeToPlan(
                        identity = identity,
                        planId = planId,
                        trialEnd = trialEnd)
            }.mapLeft {
                ApiErrorMapper.mapStorageErrorToApiError("Failed to store subscription",
                        ApiErrorCode.FAILED_TO_STORE_SUBSCRIPTION,
                        it)
            }
                    .responseBuilder(Response.Status.CREATED)
                    .build()

    /**
     * Removes a plan from the list subscriptions for a subscriber.
     */
    @DELETE
    @Path("{email}/plans/{planId}")
    @Produces("application/json")
    fun detachPlan(@PathParam("email") email: String,
                   @PathParam("planId") planId: String): Response =
            storage.getIdentityForContactEmail(contactEmail = email).flatMap { identity: Identity ->
                storage.unsubscribeFromPlan(
                        identity = identity,
                        planId = planId)
            }.mapLeft {
                ApiErrorMapper.mapStorageErrorToApiError("Failed to remove subscription",
                        ApiErrorCode.FAILED_TO_REMOVE_SUBSCRIPTION,
                        it)
            }
                    .responseBuilder()
                    .build()
}

/**
 * Resource used to handle bundles related REST calls.
 */
@Path("/bundles")
class BundlesResource {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Get all bundles for the subscriber.
     */
    @GET
    @Path("{email}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getBundlesByEmail(@Auth token: AccessTokenPrincipal?,
                          @NotNull
                          @PathParam("email")
                          email: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Accessing bundles for $email")
                getBundles(contactEmail = email)
                        .responseBuilder()
            }.build()

    // TODO: Reuse the one from SubscriberDAO
    private fun getBundles(contactEmail: String): Either<ApiError, Collection<Bundle>> {
        return try {
            storage.getIdentityForContactEmail(contactEmail = contactEmail).flatMap { identity: Identity ->
                storage.getBundles(identity)
            }.mapLeft {
                NotFoundError("Failed to get bundles. ${it.message}", ApiErrorCode.FAILED_TO_FETCH_BUNDLES)
            }
        } catch (e: Exception) {
            logger.error("Failed to get bundles for customer with contactEmail - $contactEmail", e)
            Either.left(NotFoundError("Failed to get bundles", ApiErrorCode.FAILED_TO_FETCH_BUNDLES))
        }
    }
}

/**
 * Resource used to handle purchase related REST calls.
 */
@Path("/purchases")
class PurchaseResource {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Get all purchase history for the subscriber.
     */
    @GET
    @Path("{email}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPurchaseHistoryByEmail(@Auth token: AccessTokenPrincipal?,
                                  @NotNull
                                  @PathParam("email")
                                  email: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Accessing bundles for $email")
                getPurchaseHistory(contactEmail = email)
                        .responseBuilder()
            }.build()

    // TODO: Reuse the one from SubscriberDAO
    private fun getPurchaseHistory(contactEmail: String): Either<ApiError, Collection<PurchaseRecord>> {
        return try {
            storage.getIdentityForContactEmail(contactEmail = contactEmail).flatMap { identity: Identity ->
                storage.getPurchaseRecords(identity)
            }.bimap(
                    { NotFoundError("Failed to get purchase history.", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_HISTORY, it) },
                    { it.toList() })
        } catch (e: Exception) {
            logger.error("Failed to get purchase history for customer with contactEmail - $contactEmail", e)
            Either.left(InternalServerError("Failed to get purchase history", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_HISTORY))
        }
    }
}

/**
 * Resource used to handle refund related REST calls.
 */
@Path("/refund")
class RefundResource {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Refund a specified purchase for the subscriber.
     */
    @PUT
    @Path("{email}")
    @Produces(MediaType.APPLICATION_JSON)
    fun refundPurchaseByEmail(@Auth token: AccessTokenPrincipal?,
                              @NotNull
                              @PathParam("email")
                              email: String,
                              @NotNull
                              @QueryParam("purchaseRecordId")
                              purchaseRecordId: String,
                              @NotNull
                              @QueryParam("reason")
                              reason: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Refunding purchase for $email at id: $purchaseRecordId")
                refundPurchase(email, purchaseRecordId, reason)
                        .map {
                            logger.info(NOTIFY_OPS_MARKER, "${token.name} refunded the purchase (id:$purchaseRecordId) for $email ")
                            it
                        }
                        .responseBuilder()
            }.build()

    private fun refundPurchase(contactEmail: String, purchaseRecordId: String, reason: String): Either<ApiError, ProductInfo> {
        return try {
            storage.getIdentityForContactEmail(contactEmail = contactEmail).flatMap { identity: Identity ->
                storage.refundPurchase(identity, purchaseRecordId, reason)
            }.mapLeft {
                when (it) {
                    is ForbiddenError -> org.ostelco.prime.apierror.ForbiddenError("Failed to refund purchase. ${it.description}", ApiErrorCode.FAILED_TO_REFUND_PURCHASE)
                    else -> NotFoundError("Failed to refund purchase. ${it.toString()}", ApiErrorCode.FAILED_TO_REFUND_PURCHASE)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to refund purchase for customer with contactEmail - $contactEmail, id: $purchaseRecordId", e)
            Either.left(InternalServerError("Failed to refund purchase", ApiErrorCode.FAILED_TO_REFUND_PURCHASE))
        }
    }
}

/**
 * Resource used to handle context REST call.
 */
@Path("/context")
class ContextResource {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Get context for the subscriber.
     */
    @GET
    @Path("{email}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getContextByEmail(@Auth token: AccessTokenPrincipal?,
                          @NotNull
                          @PathParam("email")
                          email: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Accessing context for $email")
                getContext(contactEmail = email)
                        .responseBuilder()
            }.build()

    // TODO: Reuse the one from SubscriberDAO
    fun getContext(contactEmail: String): Either<ApiError, Context> {
        return try {
            storage.getIdentityForContactEmail(contactEmail = contactEmail).flatMap { identity: Identity ->
                storage.getCustomer(identity).map { customer ->
                    storage.getAllRegionDetails(identity = identity)
                            .fold(
                                    { Context(customer = customer) },
                                    { regionDetailsCollection -> Context(customer = customer, regions = regionDetailsCollection) })
                }
            }.mapLeft {
                NotFoundError("Failed to fetch customer.", ApiErrorCode.FAILED_TO_FETCH_CONTEXT, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch context for customer with contactEmail - $contactEmail", e)
            Either.left(NotFoundError("Failed to fetch context", ApiErrorCode.FAILED_TO_FETCH_CONTEXT))
        }
    }
}

open class HoustonResource {
    private val logger by getLogger()

    private val storage by lazy { getResource<AdminDataSource>() }
    private val notifier by lazy { getResource<AppNotifier>() }

    fun getCustomerId(contactEmail: String): Either<ApiError, String> {
        return try {
            storage.getIdentityForContactEmail(contactEmail = contactEmail).flatMap { identity: Identity ->
                storage.getCustomer(identity = identity)
            }.mapLeft {
                NotFoundError("Did not find subscription.", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS, it)
            }.map {
                it.id
            }
        } catch (e: Exception) {
            logger.error("Did not find subscription for email $contactEmail", e)
            InternalServerError("Did not find subscription", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS).left()
        }
    }
}

/**
 * Resource used to handle notification related REST calls.
 */
@Path("/notify")
class NotifyResource: HoustonResource() {
    private val logger by getLogger()

    private val storage by lazy { getResource<AdminDataSource>() }
    private val notifier by lazy { getResource<AppNotifier>() }

    /**
     * Sends a notification to all devices for a subscriber.
     */
    @PUT
    @Path("{email}")
    @Produces(MediaType.APPLICATION_JSON)
    fun sendNotificationByEmail(@Auth token: AccessTokenPrincipal?,
                                @NotNull
                                @PathParam("email")
                                email: String,
                                @NotNull
                                @QueryParam("title")
                                title: String,
                                @NotNull
                                @QueryParam("message")
                                message: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                getCustomerId(contactEmail = email)
                        .map { customerId ->
                            logger.info("${token.name} Sending notification to $email customerId: $customerId")
                            val data = mapOf("timestamp" to "${System.currentTimeMillis()}")
                            notifier.notify(customerId, title, message, data)
                            customerId
                        }
                        .responseBuilder("Message Sent")
            }.build()
}


/**
 * Resource used to handle audit log related REST calls.
 */
@Path("/auditLog")
class AuditLogResource: HoustonResource() {

    private val logger by getLogger()

    private val storage by lazy { getResource<AdminDataSource>() }
    private val auditLogStore by lazy { getResource<AuditLogStore>() }

    /**
     * Fetch all audit logs for a subscriber.
     */
    @GET
    @Path("{email}")
    @Produces(MediaType.APPLICATION_JSON)
    fun query(@Auth token: AccessTokenPrincipal?,
              @NotNull
              @PathParam("email")
              email: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                getCustomerId(contactEmail = email)
                        .flatMap { customerId ->
                            logger.info("${token.name} fetching audit log of $email customerId: $customerId")
                            auditLogStore.getCustomerActivityHistory(customerId = customerId)
                                    .mapLeft { errorMessage ->
                                        InternalServerError(errorMessage, FAILED_TO_FETCH_AUDIT_LOGS)
                                    }
                        }
                        .responseBuilder()
            }.build()
}

/**
 * Resource used to manipulate customer record.
 */
@Path("/customer")
class CustomerResource {

    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Remove customer from Prime.
     */
    @DELETE
    @Path("{email}")
    @Produces(MediaType.APPLICATION_JSON)
    fun removeCustomer(@Auth token: AccessTokenPrincipal?,
              @NotNull
              @PathParam("email")
              email: String): Response =
            if (token == null) {
                Response.status(Response.Status.UNAUTHORIZED)
            } else {
                logger.info("${token.name} Removing the customer for $email")
                removeCustomer(contactEmail = email)
                        .responseBuilder(Response.Status.NO_CONTENT)
            }.build()

    // TODO: Reuse the one from SubscriberDAO
    private fun removeCustomer(contactEmail: String): Either<ApiError, Unit> {
        return try {
            storage.getIdentityForContactEmail(contactEmail = contactEmail).flatMap { identity: Identity ->
                storage.removeCustomer(identity)
            }.mapLeft {
                NotFoundError("Failed to remove customer.", ApiErrorCode.FAILED_TO_REMOVE_CUSTOMER, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile for customer with contactEmail - $contactEmail", e)
            Either.left(NotFoundError("Failed to remove customer", ApiErrorCode.FAILED_TO_REMOVE_CUSTOMER))
        }
    }
}
