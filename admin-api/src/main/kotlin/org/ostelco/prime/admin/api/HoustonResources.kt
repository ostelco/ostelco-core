package org.ostelco.prime.admin.api

import arrow.core.Either
import io.dropwizard.auth.Auth
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.ApiErrorMapper
import org.ostelco.prime.apierror.InternalServerError
import org.ostelco.prime.apierror.NotFoundError
import org.ostelco.prime.appnotifier.AppNotifier
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.Customer
import org.ostelco.prime.model.Identity
import org.ostelco.prime.model.Plan
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.model.Subscription
import org.ostelco.prime.module.getResource
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.storage.AdminDataSource
import java.net.URLDecoder
import java.util.regex.Pattern
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
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
                   id: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }
        return if (!isEmail(id)) {
            logger.info("${token.name} Accessing profile for msisdn:$id")
            getProfileForMsisdn(id).fold(
                    { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                    { Response.status(Response.Status.OK).entity(asJson(it)) })
                    .build()
        } else {
            logger.info("${token.name} Accessing profile for email:$id")
            getProfile(Identity(id, "EMAIL", "email")).fold(
                    { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                    { Response.status(Response.Status.OK).entity(asJson(it)) })
                    .build()
        }
    }

    /**
     * Get the subscriptions for this subscriber.
     */
    @GET
    @Path("{email}/subscriptions")
    @Produces(MediaType.APPLICATION_JSON)
    fun getSubscriptions(@Auth token: AccessTokenPrincipal?,
                         @NotNull
                         @PathParam("email")
                         email: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }
        logger.info("${token.name} Accessing subscriptions for email:$email")
        return getSubscriptions(Identity(email, "EMAIL", "email")).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    /**
     * Get all the eKYC scan information for this subscriber.
     */
    @GET
    @Path("{email}/scans")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAllScanInformation(@Auth token: AccessTokenPrincipal?,
                              @NotNull
                              @PathParam("email")
                              email: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }
        logger.info("${token.name} Accessing scan information for email:$email")
        return getAllScanInformation(identity = Identity(id = email, type = "EMAIL", provider = "email")).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    private fun getAllScanInformation(identity: Identity): Either<ApiError, Collection<ScanInformation>> {
        return try {
            storage.getAllScanInformation(identity = identity).mapLeft {
                NotFoundError("Failed to fetch scan information.", ApiErrorCode.FAILED_TO_FETCH_SCAN_INFORMATION, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch scan information for customer with identity - $identity", e)
            Either.left(NotFoundError("Failed to fetch scan information", ApiErrorCode.FAILED_TO_FETCH_SCAN_INFORMATION))
        }
    }

    // TODO: Reuse the one from SubscriberDAO
    private fun getProfile(identity: Identity): Either<ApiError, Customer> {
        return try {
            storage.getCustomer(identity).mapLeft {
                NotFoundError("Failed to fetch profile.", ApiErrorCode.FAILED_TO_FETCH_CUSTOMER, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile for customer with identity - $identity", e)
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
    private fun getSubscriptions(identity: Identity): Either<ApiError, Collection<Subscription>> {
        return try {
            storage.getSubscriptions(identity).mapLeft {
                NotFoundError("Failed to get subscriptions.", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to get subscriptions for customer with identity - $identity", e)
            Either.left(InternalServerError("Failed to get subscriptions", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS))
        }
    }

    /**
     * Fetches and return all plans that a subscriber subscribes
     * to if any.
     */
    @GET
    @Path("{email}/plans")
    @Produces("application/json")
    fun getPlans(@PathParam("email") email: String): Response {
        return storage.getPlans(identity = Identity(id = email, type = "EMAIL", provider = "email")).fold(
                {
                    val err = ApiErrorMapper.mapStorageErrorToApiError("Failed to fetch plans",
                            ApiErrorCode.FAILED_TO_FETCH_PLANS_FOR_SUBSCRIBER,
                            it)
                    Response.status(err.status).entity(asJson(err))
                },
                { Response.status(Response.Status.OK).entity(asJson(it)) }
        ).build()
    }

    /**
     * Attaches (subscribes) a subscriber to a plan.
     */
    @POST
    @Path("{email}/plans/{planId}")
    @Produces("application/json")
    fun attachPlan(@PathParam("email") email: String,
                   @PathParam("planId") planId: String,
                   @QueryParam("trial_end") trialEnd: Long): Response {
        return storage.subscribeToPlan(
                identity = Identity(id = email, type = "EMAIL", provider = "email"),
                planId = planId,
                trialEnd = trialEnd).fold(
                {
                    val err = ApiErrorMapper.mapStorageErrorToApiError("Failed to store subscription",
                            ApiErrorCode.FAILED_TO_STORE_SUBSCRIPTION,
                            it)
                    Response.status(err.status).entity(asJson(err))
                },
                { Response.status(Response.Status.CREATED) }
        ).build()
    }

    /**
     * Removes a plan from the list subscriptions for a subscriber.
     */
    @DELETE
    @Path("{email}/plans/{planId}")
    @Produces("application/json")
    fun detachPlan(@PathParam("email") email: String,
                   @PathParam("planId") planId: String): Response {
        return storage.unsubscribeFromPlan(
                identity = Identity(id = email, type = "EMAIL", provider = "email"),
                planId = planId).fold(
                {
                    val err = ApiErrorMapper.mapStorageErrorToApiError("Failed to remove subscription",
                            ApiErrorCode.FAILED_TO_REMOVE_SUBSCRIPTION,
                            it)
                    Response.status(err.status).entity(asJson(err))
                },
                { Response.status(Response.Status.OK) }
        ).build()
    }
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
                          email: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }
        logger.info("${token.name} Accessing bundles for $email")
        return getBundles(Identity(email, "EMAIL", "email")).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    // TODO: Reuse the one from SubscriberDAO
    private fun getBundles(identity: Identity): Either<ApiError, Collection<Bundle>> {
        return try {
            storage.getBundles(identity).mapLeft {
                NotFoundError("Failed to get bundles. ${it.message}", ApiErrorCode.FAILED_TO_FETCH_BUNDLES)
            }
        } catch (e: Exception) {
            logger.error("Failed to get bundles for customer with identity - $identity", e)
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
                                  email: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }
        logger.info("${token.name} Accessing bundles for $email")
        return getPurchaseHistory(Identity(email, "EMAIL", "email")).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    // TODO: Reuse the one from SubscriberDAO
    private fun getPurchaseHistory(identity: Identity): Either<ApiError, Collection<PurchaseRecord>> {
        return try {
            return storage.getPurchaseRecords(identity).bimap(
                    { NotFoundError("Failed to get purchase history.", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_HISTORY, it) },
                    { it.toList() })
        } catch (e: Exception) {
            logger.error("Failed to get purchase history for customer with identity - $identity", e)
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
                              reason: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }
        logger.info("${token.name} Refunding purchase for $email at id: $purchaseRecordId")
        return refundPurchase(Identity(email, "EMAIL", "email"), purchaseRecordId, reason).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                {
                    logger.info(NOTIFY_OPS_MARKER, "${token.name} refunded the purchase (id:$purchaseRecordId) for $email ")
                    Response.status(Response.Status.OK).entity(asJson(it))
                })
                .build()
    }

    private fun refundPurchase(identity: Identity, purchaseRecordId: String, reason: String): Either<ApiError, ProductInfo> {
        return try {
            return storage.refundPurchase(identity, purchaseRecordId, reason).mapLeft {
                when (it) {
                    is ForbiddenError -> org.ostelco.prime.apierror.ForbiddenError("Failed to refund purchase. ${it.description}", ApiErrorCode.FAILED_TO_REFUND_PURCHASE)
                    else -> NotFoundError("Failed to refund purchase. ${it.description}", ApiErrorCode.FAILED_TO_REFUND_PURCHASE)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to refund purchase for customer with identity - $identity, id: $purchaseRecordId", e)
            Either.left(InternalServerError("Failed to refund purchase", ApiErrorCode.FAILED_TO_REFUND_PURCHASE))
        }
    }
}

/**
 * Resource used to handle notification related REST calls.
 */
@Path("/notify")
class NotifyResource {
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
                                message: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }
        return getCustomerId(email = email).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { customerId ->
                    logger.info("${token.name} Sending notification to $email customerId: $customerId")
                    val data = mapOf("timestamp" to "${System.currentTimeMillis()}")
                    notifier.notify(customerId, title, message, data)
                    Response.status(Response.Status.OK).entity("Message Sent")
                })
                .build()

    }

    private fun getCustomerId(email: String): Either<ApiError, String> {
        return try {
            storage.getCustomerId(identity = Identity(id = email, type = "EMAIL", provider = "email")).mapLeft {
                NotFoundError("Did not find msisdn for this subscription.", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS, it)
            }
        } catch (e: Exception) {
            logger.error("Did not find msisdn for email $email", e)
            Either.left(InternalServerError("Did not find subscription", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS))
        }
    }
}

/**
 * Resource used to handle plans related REST calls.
 */
@Path("/plans")
class PlanResource {

    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Return plan details.
     */
    @GET
    @Path("{planId}")
    @Produces("application/json")
    fun get(@NotNull
            @PathParam("planId") planId: String): Response {
        return storage.getPlan(planId).fold(
                {
                    val err = ApiErrorMapper.mapStorageErrorToApiError("Failed to fetch plan",
                            ApiErrorCode.FAILED_TO_FETCH_PLAN,
                            it)
                    Response.status(err.status).entity(asJson(err))
                },
                { Response.status(Response.Status.OK).entity(asJson(it)) }
        ).build()
    }

    /**
     * Creates a plan.
     */
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    fun create(plan: Plan): Response {
        return storage.createPlan(plan).fold(
                {
                    val err = ApiErrorMapper.mapStorageErrorToApiError("Failed to store plan",
                            ApiErrorCode.FAILED_TO_STORE_PLAN,
                            it)
                    Response.status(err.status).entity(asJson(err))
                },
                { Response.status(Response.Status.CREATED).entity(asJson(it)) }
        ).build()
    }

    /**
     * Deletes a plan.
     * Note, will fail if there are subscriptions on the plan.
     */
    @DELETE
    @Path("{planId}")
    @Produces("application/json")
    fun delete(@NotNull
               @PathParam("planId") planId: String): Response {
        return storage.deletePlan(planId).fold(
                {
                    val err = ApiErrorMapper.mapStorageErrorToApiError("Failed to remove plan",
                            ApiErrorCode.FAILED_TO_REMOVE_PLAN,
                            it)
                    Response.status(err.status).entity(asJson(err))
                },
                { Response.status(Response.Status.OK).entity(asJson(it)) }
        ).build()
    }
}
