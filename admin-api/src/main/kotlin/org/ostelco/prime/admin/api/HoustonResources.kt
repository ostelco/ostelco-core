package org.ostelco.prime.admin.api

import arrow.core.Either
import io.dropwizard.auth.Auth
import org.ostelco.prime.apierror.*
import org.ostelco.prime.appnotifier.AppNotifier
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.*
import org.ostelco.prime.module.getResource
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.storage.AdminDataSource
import org.ostelco.prime.storage.AlreadyExistsError
import org.ostelco.prime.storage.NotCreatedError
import org.ostelco.prime.storage.NotDeletedError
import java.net.URLDecoder
import java.util.regex.Pattern
import javax.validation.constraints.NotNull
import javax.ws.rs.*
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
        val decodedId = URLDecoder.decode(id, "UTF-8")
        if (!isEmail(decodedId)) {
            logger.info("${token.name} Accessing profile for msisdn:$decodedId")
            return getProfileForMsisdn(decodedId).fold(
                    { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                    { Response.status(Response.Status.OK).entity(asJson(it)) })
                    .build()
        } else {
            logger.info("${token.name} Accessing profile for email:$decodedId")
            return getProfile(decodedId).fold(
                    { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                    { Response.status(Response.Status.OK).entity(asJson(it)) })
                    .build()
        }
    }

    /**
     * Get the subscriber state/
     */
    @GET
    @Path("{email}/state")
    @Produces(MediaType.APPLICATION_JSON)
    fun getSubscriberState(@Auth token: AccessTokenPrincipal?,
                           @NotNull
                           @PathParam("email")
                           email: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }
        val decodedId = URLDecoder.decode(email, "UTF-8")
        logger.info("${token.name} Accessing state for email:$decodedId")
        return getSubscriberState(decodedId).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
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
        val decodedId = URLDecoder.decode(email, "UTF-8")
        logger.info("${token.name} Accessing subscriptions for email:$decodedId")
        return getSubscriptions(decodedId).fold(
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
        val decodedId = URLDecoder.decode(email, "UTF-8")
        logger.info("${token.name} Accessing scan information for email:$decodedId")
        return getAllScanInformation(decodedId).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    private fun getAllScanInformation(subscriberId: String): Either<ApiError, Collection<ScanInformation>> {
        return try {
            storage.getAllScanInformation(subscriberId).mapLeft {
                NotFoundError("Failed to fetch scan information.", ApiErrorCode.FAILED_TO_FETCH_SCAN_INFORMATION, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch scan information $subscriberId", e)
            Either.left(NotFoundError("Failed to fetch scan information", ApiErrorCode.FAILED_TO_FETCH_SCAN_INFORMATION))
        }
    }

    // TODO: Reuse the one from SubscriberDAO
    private fun getProfile(subscriberId: String): Either<ApiError, Subscriber> {
        return try {
            storage.getSubscriber(subscriberId).mapLeft {
                NotFoundError("Failed to fetch profile.", ApiErrorCode.FAILED_TO_FETCH_PROFILE, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile for subscriberId $subscriberId", e)
            Either.left(NotFoundError("Failed to fetch profile", ApiErrorCode.FAILED_TO_FETCH_PROFILE))
        }
    }

    private fun isEmail(email: String): Boolean {
        val regex = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$"
        val pattern = Pattern.compile(regex)
        return pattern.matcher(email).matches();
    }

    private fun getProfileForMsisdn(msisdn: String): Either<ApiError, Subscriber> {
        return try {
            storage.getSubscriberForMsisdn(msisdn).mapLeft {
                NotFoundError("Failed to fetch profile.", ApiErrorCode.FAILED_TO_FETCH_PROFILE, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile for msisdn $msisdn", e)
            Either.left(NotFoundError("Failed to fetch profile", ApiErrorCode.FAILED_TO_FETCH_PROFILE))
        }
    }

    // TODO: Reuse the one from SubscriberDAO
    private fun getSubscriptions(subscriberId: String): Either<ApiError, Collection<Subscription>> {
        try {
            return storage.getSubscriptions(subscriberId).mapLeft {
                NotFoundError("Failed to get subscriptions.", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to get subscriptions for subscriberId $subscriberId", e)
            return Either.left(BadGatewayError("Failed to get subscriptions", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS))
        }
    }
    // TODO: Reuse the one from SubscriberDAO
    private fun getSubscriberState(subscriberId: String): Either<ApiError, SubscriberState> {
        try {
            return storage.getSubscriberState(subscriberId).mapLeft {
                NotFoundError("Failed to fetch state of subscriber.", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIBER_STATE, it)
            }
        } catch (e: Exception) {
            logger.error("Error fetching state for subscriberId $subscriberId", e)
            return Either.left(BadGatewayError("Error fetching state", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIBER_STATE))
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
        return storage.getPlans(email).fold(
                { err ->
                    when (err) {
                        is org.ostelco.prime.storage.NotFoundError -> Response.status(Response.Status.NOT_FOUND).entity(asJson(err))
                        else -> {
                            logger.error("Unexpected error ${err} in 'get-plans' requests for ${email}")
                            Response.status(Response.Status.BAD_GATEWAY).entity(asJson(err))
                        }
                    }
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
        return storage.subscribeToPlan(email, planId, trialEnd).fold(
                { err ->
                    when (err) {
                        is org.ostelco.prime.storage.NotFoundError -> Response.status(Response.Status.NOT_FOUND).entity(asJson(err))
                        is NotCreatedError -> Response.status(Response.Status.BAD_REQUEST).entity(asJson(err))
                        else -> {
                            logger.error("Unexpected error ${err} in 'attac-plan' requests for ${email} -> ${planId}")
                            Response.status(Response.Status.BAD_GATEWAY).entity(asJson(err))
                        }
                    }
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
        return storage.unsubscribeFromPlan(email, planId).fold(
                {err ->
                    when (err) {
                        is org.ostelco.prime.storage.NotFoundError -> Response.status(Response.Status.NOT_FOUND).entity(asJson(err))
                        is NotDeletedError -> Response.status(Response.Status.BAD_REQUEST).entity(asJson(err))
                        else -> {
                            logger.error("Unexpected error ${err} in 'detach-plan' requests for ${email} -> ${planId}")
                            Response.status(Response.Status.BAD_GATEWAY).entity(asJson(err))
                        }
                    }
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
        val decodedEmail = URLDecoder.decode(email, "UTF-8")
        logger.info("${token.name} Accessing bundles for $decodedEmail")
        return getBundles(decodedEmail).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    // TODO: Reuse the one from SubscriberDAO
    private fun getBundles(subscriberId: String): Either<ApiError, Collection<Bundle>> {
        return try {
            storage.getBundles(subscriberId).mapLeft {
                NotFoundError("Failed to get bundles. ${it.message}", ApiErrorCode.FAILED_TO_FETCH_BUNDLES)
            }
        } catch (e: Exception) {
            logger.error("Failed to get bundles for subscriberId $subscriberId", e)
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
        val decodedEmail = URLDecoder.decode(email, "UTF-8")
        logger.info("${token.name} Accessing bundles for $decodedEmail")
        return getPurchaseHistory(decodedEmail).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    // TODO: Reuse the one from SubscriberDAO
    private fun getPurchaseHistory(subscriberId: String): Either<ApiError, Collection<PurchaseRecord>> {
        return try {
            return storage.getPurchaseRecords(subscriberId).bimap(
                    { NotFoundError("Failed to get purchase history.", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_HISTORY, it) },
                    { it.toList() })
        } catch (e: Exception) {
            logger.error("Failed to get purchase history for subscriberId $subscriberId", e)
            Either.left(BadGatewayError("Failed to get purchase history", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_HISTORY))
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
        val decodedEmail = URLDecoder.decode(email, "UTF-8")
        logger.info("${token.name} Refunding purchase for $decodedEmail at id: $purchaseRecordId")
        return refundPurchase(decodedEmail, purchaseRecordId, reason).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                {
                    logger.info(NOTIFY_OPS_MARKER, "${token.name} refunded the purchase (id:$purchaseRecordId) for $decodedEmail ")
                    Response.status(Response.Status.OK).entity(asJson(it))
                })
                .build()
    }

    private fun refundPurchase(subscriberId: String, purchaseRecordId: String, reason: String): Either<ApiError, ProductInfo> {
        return try {
            return storage.refundPurchase(subscriberId, purchaseRecordId, reason).mapLeft {
                when(it) {
                    is ForbiddenError -> org.ostelco.prime.apierror.ForbiddenError("Failed to refund purchase. ${it.description}", ApiErrorCode.FAILED_TO_REFUND_PURCHASE)
                    else -> NotFoundError("Failed to refund purchase. ${it.description}", ApiErrorCode.FAILED_TO_REFUND_PURCHASE)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to refund purchase for subscriberId $subscriberId, id: $purchaseRecordId", e)
            Either.left(BadGatewayError("Failed to refund purchase", ApiErrorCode.FAILED_TO_REFUND_PURCHASE))
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
        val decodedEmail = URLDecoder.decode(email, "UTF-8")
        return getMsisdn(decodedEmail).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { msisdn ->
                    logger.info("${token.name} Sending notification to $decodedEmail msisdn: $msisdn")
                    notifier.notify(msisdn, title, message)
                    Response.status(Response.Status.OK).entity("Message Sent")
                })
                .build()

    }

    // TODO: Reuse the one from SubscriberDAO
    private fun getMsisdn(subscriberId: String): Either<ApiError, String> {
        return try {
            storage.getMsisdn(subscriberId).mapLeft {
                NotFoundError("Did not find msisdn for this subscription.", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS, it)
            }
        } catch (e: Exception) {
            logger.error("Did not find msisdn for subscriberId $subscriberId", e)
            Either.left(BadGatewayError("Did not find subscription", ApiErrorCode.FAILED_TO_FETCH_SUBSCRIPTIONS))
        }
    }
}

/**
 * Resource used to handle plans related REST calls.
 */
@Path("/plans")
class PlanResource() {

    private val logger by getLogger()
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
                { err ->
                    when (err) {
                        is org.ostelco.prime.storage.NotFoundError -> Response.status(Response.Status.NOT_FOUND).entity(asJson(err))
                        else -> {
                            logger.error("Unexpected error ${err} in 'get-plan' requests for plan ${planId}")
                            Response.status(Response.Status.BAD_GATEWAY).entity(asJson(err))
                        }
                    }
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
                { err ->
                    when (err) {
                        is AlreadyExistsError -> Response.status(Response.Status.BAD_REQUEST).entity(asJson(err))
                        is NotCreatedError -> Response.status(Response.Status.BAD_GATEWAY).entity(asJson(err))
                        else -> {
                            logger.error("Unexpected error ${err} in 'create-plan' requests for plan ${plan}")
                            Response.status(Response.Status.BAD_GATEWAY).entity(asJson(err))
                        }
                    }
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
               @PathParam("planId") planId: String) : Response {
        return storage.deletePlan(planId).fold(
                { err ->
                    when (err) {
                        is org.ostelco.prime.storage.NotFoundError -> Response.status(Response.Status.BAD_REQUEST).entity(asJson(err))
                        is NotDeletedError -> Response.status(Response.Status.BAD_GATEWAY).entity(asJson(err))
                        else -> {
                            logger.error("Unexpected error ${err} in 'delete-plan' requests for plan ${planId}")
                            Response.status(Response.Status.BAD_GATEWAY).entity(asJson(err))
                        }
                    }
                },
                { Response.status(Response.Status.OK).entity(asJson(it))}
        ).build()
    }
}
