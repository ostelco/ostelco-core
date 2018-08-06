package org.ostelco.prime.client.api.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.client.api.auth.AccessTokenPrincipal
import org.ostelco.prime.client.api.store.SubscriberDAO
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.Response

/**
 * Analytics API.
 *
 */
@Path("/analytics")
class AnalyticsResource(private val dao: SubscriberDAO) {

    @POST
    @Consumes("application/json")
    fun report(@Auth token: AccessTokenPrincipal?,
               @NotNull event: String): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        val error = dao.reportAnalytics(token.name, event)

        return if (error.isEmpty) {
            Response.status(Response.Status.CREATED)
                    .build()
        } else {
            Response.status(Response.Status.NOT_FOUND)
                    .entity(asJson(error.get()))
                    .build()
        }
    }
}
