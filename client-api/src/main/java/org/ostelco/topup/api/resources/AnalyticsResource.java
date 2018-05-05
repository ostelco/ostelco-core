package org.ostelco.topup.api.resources;

import io.dropwizard.auth.Auth;
import io.vavr.control.Option;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.db.SubscriberDAO;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Analytics API.
 *
 */
@Path("/analytics")
public class AnalyticsResource extends ResourceHelpers {

    private final SubscriberDAO dao;

    public AnalyticsResource(SubscriberDAO dao) {
        this.dao = dao;
    }

    @POST
    @Consumes({"application/json"})
    public Response report(@Auth AccessTokenPrincipal token,
            @NotNull final String event) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Option<Error> error = dao.reportAnalytics(token.getName(), event);

        return error.isEmpty()
            ? Response.status(Response.Status.CREATED)
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .entity(asJson(error.get()))
                 .build();
    }
}
