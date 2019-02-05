package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Identity
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Analytics API.
 *
 */
@Path("/analytics")
class AnalyticsResource(private val dao: SubscriberDAO) {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun report(@Auth token: AccessTokenPrincipal?,
               @NotNull event: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.reportAnalytics(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                events = event)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { Response.status(Response.Status.CREATED) })
                .build()
    }
}
