package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Identity
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.DeprecationLevel.WARNING

/**
 * Subscriptions API.
 *
 */

@Deprecated(
        message = "Will be removed in later releases.",
        replaceWith = ReplaceWith("CustomerResource", imports = arrayOf("org.ostelco.prime.client.api.resources.CustomerResource")),
        level = WARNING)
@Path("/subscription")
class SubscriptionResource(private val dao: SubscriberDAO) {

    @GET
    @Path("activePseudonyms")
    @Produces(MediaType.APPLICATION_JSON)
    fun getActivePseudonyms(@Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getActivePseudonymForSubscriber(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider))
                .fold(
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

        return dao.getSubscriptions(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider))
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.OK).entity(asJson(it)) })
                .build()
    }
}
