package org.ostelco.prime.admin.api

import arrow.core.Either
import io.dropwizard.auth.Auth
import org.ostelco.prime.apierror.ApiError
import org.ostelco.prime.apierror.ApiErrorCode
import org.ostelco.prime.apierror.NotFoundError
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Subscriber
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientDataSource
import java.net.URLDecoder
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/profile")
class ProfileResource() {
    private val logger by getLogger()
    private val storage by lazy { getResource<ClientDataSource>() }

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
