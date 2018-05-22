package org.ostelco.topup.api.resources;

import io.dropwizard.auth.Auth;
import io.vavr.control.Either;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.ostelco.prime.client.api.model.SubscriptionStatus;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.db.SubscriberDAO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

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
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Either<Error, SubscriptionStatus> result = dao.getSubscriptionStatus(token.getName());

        return result.isRight()
            ? Response.status(Response.Status.OK)
                 .entity(getSubscriptionStatusAsJson(result.right().get()))
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .build();
    }
}
