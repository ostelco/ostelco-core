package org.ostelco.prime.admin.api

import arrow.core.Either
import io.dropwizard.auth.Auth
import org.ostelco.prime.admin.importer.UpdateSegments
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.BadGatewayError
import org.ostelco.prime.apierror.NotFoundError
import org.ostelco.prime.appnotifier.AppNotifier
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.module.getResource
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import org.ostelco.prime.paymentprocessor.core.ForbiddenError
import org.ostelco.prime.paymentprocessor.core.ProductInfo
import org.ostelco.prime.paymentprocessor.core.ProfileInfo
import org.ostelco.prime.storage.AdminDataSource
import org.ostelco.prime.storage.ClientDataSource
import java.net.URLDecoder
import javax.validation.constraints.NotNull
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Resource used to handle the profile related REST calls.
 */
@Path("/profile")
class ProfileResource() {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Get the subscriber profile.
     */
    @GET
    @Path("email/{email}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getProfileByEmail(@Auth token: AccessTokenPrincipal?,
                          @NotNull
                          @PathParam("email")
                          email: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }
        val decodedEmail = URLDecoder.decode(email, "UTF-8")
        logger.info("${token.name} Accessing profile for $decodedEmail")
        return getProfile(decodedEmail).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    // TODO: Reuse the one from SubscriberDAO
    private fun getProfile(subscriberId: String): Either<ApiError, Subscriber> {
        return try {
            storage.getSubscriber(subscriberId).mapLeft {
                NotFoundError("Failed to fetch profile.", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_PROFILE, it)
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch profile for subscriberId $subscriberId", e)
            Either.left(NotFoundError("Failed to fetch profile", ApiErrorCode.FAILED_TO_FETCH_PAYMENT_PROFILE))
        }
    }

}

/**
 * Resource used to handle bundles related REST calls.
 */
@Path("/bundles")
class BundlesResource() {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Get all bundles for the subscriber.
     */
    @GET
    @Path("email/{email}")
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
class PurchaseResource() {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Get all purchase history for the subscriber.
     */
    @GET
    @Path("email/{email}")
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
 * Resource used to handle refunds related REST calls.
 */
@Path("/refunds")
class RefundsResource() {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

    /**
     * Refund a specified purchase for the subscriber.
     */
    @PUT
    @Path("email/{email}")
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
class NotifyResource() {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }
    private val notifier by lazy { getResource<AppNotifier>() }
    /**
     * Sends a notification to all devices for a subscriber.
     */
    @PUT
    @Path("email/{email}")
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
                NotFoundError("Did not find msisdn for this subscription.", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN, it)
            }
        } catch (e: Exception) {
            logger.error("Did not find msisdn for subscriberId $subscriberId", e)
            Either.left(BadGatewayError("Did not find subscription", ApiErrorCode.FAILED_TO_STORE_APPLICATION_TOKEN))
        }
    }
}
