package org.ostelco.topup.api.resources;

import io.dropwizard.auth.Auth;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.ostelco.prime.client.api.model.Consent;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.db.SubscriberDAO;

import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Consents API.
 *
 */
@AllArgsConstructor
@Path("/consents")
public class ConsentsResource extends ResourceHelpers {

    @NonNull
    private final SubscriberDAO dao;

    @GET
    @Produces({"application/json"})
    public Response getConsents(@Auth AccessTokenPrincipal token) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Either<Error, List<Consent>> result = dao.getConsents(token.getName());

        return result.isRight()
            ? Response.status(Response.Status.OK)
                 .entity(getConsentsAsJson(result.right().get()))
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .entity(getErrorAsJson(result.left().get()))
                 .build();
    }

    @PUT
    @Path("{consent-id}")
    @Produces({"application/json"})
    public Response updateConsent(@Auth AccessTokenPrincipal token,
            @NotNull
            @PathParam("consent-id") String consentId,
            @DefaultValue("true") @QueryParam("accepted") boolean accepted) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Option<Error> error = accepted
            ? dao.acceptConsent(token.getName(), consentId)
            : dao.rejectConsent(token.getName(), consentId);

        return error.isEmpty()
            ? Response.status(Response.Status.OK)
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .entity(getErrorAsJson(error.get()))
                 .build();
    }
}
