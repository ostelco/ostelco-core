package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.core.EndpointUserInfo;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.db.SubscriberDAO;

import io.dropwizard.auth.Auth;
import io.vavr.control.Option;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Analytics API.
 *
 */
@AllArgsConstructor
@Path("/analytics")
public class AnalyticsResource extends ResourceHelpers {

    @NonNull
    private final SubscriberDAO dao;

    @POST
    @Consumes({"application/json"})
    public Response report(@Auth AccessTokenPrincipal token,
            @Valid @HeaderParam("X-Endpoint-API-UserInfo") EndpointUserInfo userInfo,
            final String events) {

        Option<Error> error = dao.reportAnalytics(token.getName(), events);

        return error.isEmpty()
            ? Response.status(Response.Status.CREATED)
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .entity(getErrorAsJson(error.get()))
                 .build();
    }
}
