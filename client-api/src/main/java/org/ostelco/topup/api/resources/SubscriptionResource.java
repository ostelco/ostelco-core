package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.core.SubscriptionStatus;
import org.ostelco.topup.api.db.SubscriberDAO;

import io.dropwizard.auth.Auth;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import io.vavr.control.Either;

/**
 * Subscriptions API.
 *
 */
@AllArgsConstructor
@Path("/subscription")
public class SubscriptionResource extends ResourceHelpers {

    @NonNull
    private final SubscriberDAO dao;

    @GET
    @Path("status")
    @Produces({"application/json"})
    public Response getSubscription(@Auth AccessTokenPrincipal token) {

        Either<Error, SubscriptionStatus> result = dao.getSubscriptionStatus(token.getName());

        return result.isRight()
            ? Response.status(Response.Status.OK)
                 .entity(getSubscriptionStatusAsJson(result.right().get()))
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .build();
    }
}
