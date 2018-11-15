package org.ostelco.prime.admin.api

import arrow.core.Either
import io.dropwizard.auth.Auth
import org.ostelco.prime.admin.importer.UpdateSegments
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.BadGatewayError
import org.ostelco.prime.apierror.NotFoundError
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Bundle
import org.ostelco.prime.model.PurchaseRecord
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.module.getResource
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

@Path("/profile")
class ProfileResource() {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

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

@Path("/bundles")
class BundlesResource() {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

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

@Path("/purchases")
class PurchaseResource() {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

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

@Path("/refunds")
class RefundsResource() {
    private val logger by getLogger()
    private val storage by lazy { getResource<AdminDataSource>() }

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
        val decodedPurchaseRecordId = URLDecoder.decode(purchaseRecordId, "UTF-8")
        val decodedReason = URLDecoder.decode(reason, "UTF-8")
        logger.info("${token.name} Refunding purchase for $decodedEmail at id: $purchaseRecordId")
        return refundPurchase(decodedEmail, decodedPurchaseRecordId, decodedReason).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
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
