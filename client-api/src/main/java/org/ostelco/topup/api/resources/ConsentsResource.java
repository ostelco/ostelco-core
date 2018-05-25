package org.ostelco.topup.api.resources;

import io.dropwizard.auth.Auth;
import io.vavr.control.Either;
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
import java.util.Collection;

/**
 * Consents API.
 */
@Path("/consents")
public class ConsentsResource extends ResourceHelpers {

    private final SubscriberDAO dao;

    public ConsentsResource(SubscriberDAO dao) {
        this.dao = dao;
    }

    @GET
    @Produces({"application/json"})
    public Response getConsents(@Auth AccessTokenPrincipal token) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build();
        }

        Either<Error, Collection<Consent>> result = dao.getConsents(token.getName());

        return result.isRight()
                ? Response.status(Response.Status.OK)
                .entity(asJson(result.right().get()))
                .build()
                : Response.status(Response.Status.NOT_FOUND)
                .entity(asJson(result.left().get()))
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

        Either<Error, Consent> result = accepted
                ? dao.acceptConsent(token.getName(), consentId)
                : dao.rejectConsent(token.getName(), consentId);

        return result.isRight()
                ? Response.status(Response.Status.OK)
                .entity(asJson(result.right().get()))
                .build()
                : Response.status(Response.Status.NOT_FOUND)
                .entity(asJson(result.left().get()))
                .build();
    }
}
