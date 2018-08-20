package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.client.Client
import javax.ws.rs.core.Response

/**
 * Subscriptions API.
 *
 */

@Path("/subscription")
@Deprecated("use SubscriptionsResource", ReplaceWith("SubscriptionsResource", "org.ostelco.prime.client.api.resources.SubscriptionsResource"))
class SubscriptionResource(private val dao: SubscriberDAO,
                           val client: Client,
                           private val pseudonymEndpoint: String) {

    @GET
    @Path("status")
    @Produces("application/json")
    fun getSubscriptionStatus(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getSubscriptionStatus(token.name).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError.description)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }

    @GET
    @Path("activePseudonyms")
    @Produces("application/json")
    fun getActivePseudonyms(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getMsisdn(token.name).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError.description)).build() },
                { msisdn -> client.target("$pseudonymEndpoint/pseudonym/active/$msisdn").request().get() })
    }
}

@Path("/subscriptions")
class SubscriptionsResource(private val dao: SubscriberDAO) {

    @GET
    @Produces("application/json")
    fun getSubscription(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getSubscriptions(token.name).fold(
                { apiError -> Response.status(apiError.status).entity(asJson(apiError.description)) },
                { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }
}
