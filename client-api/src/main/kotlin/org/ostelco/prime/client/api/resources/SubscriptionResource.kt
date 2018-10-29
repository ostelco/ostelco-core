package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Subscriptions API.
 *
 */

@Path("/subscription")
@Deprecated("use SubscriptionsResource and/or BundlesResource", ReplaceWith("SubscriptionsResource", "org.ostelco.prime.client.api.resources.SubscriptionsResource"))
class SubscriptionResource(private val dao: SubscriberDAO) {

    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    fun getSubscriptionStatus(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getSubscriptionStatus(token.name).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @GET
    @Path("activePseudonyms")
    @Produces(MediaType.APPLICATION_JSON)
    fun getActivePseudonyms(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getActivePseudonymForSubscriber(token.name).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { pseudonym -> Response.status(Response.Status.OK).entity(pseudonym) })
                .build()
    }
}

@Path("/subscriptions")
class SubscriptionsResource(private val dao: SubscriberDAO) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getSubscription(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getSubscriptions(token.name).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }
}
