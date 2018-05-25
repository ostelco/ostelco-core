package org.ostelco.topup.api.resources;

import io.dropwizard.auth.Auth;
import io.vavr.control.Either;
import org.ostelco.prime.model.Subscriber;
import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.db.SubscriberDAO;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Profile API.
 *
 */
@Path("/profile")
public class ProfileResource extends ResourceHelpers {

    private final SubscriberDAO dao;

    public ProfileResource(SubscriberDAO dao) {
        this.dao = dao;
    }

    @GET
    @Produces({"application/json"})
    public Response getProfile(@Auth AccessTokenPrincipal token) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Either<Error, Subscriber> result = dao.getProfile(token.getName());

        return result.isRight()
            ? Response.status(Response.Status.OK)
                 .entity(asJson(result.right().get()))
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .build();
    }

    @POST
    @Consumes({"application/json"})
    public Response createProfile(@Auth AccessTokenPrincipal token,
            @NotNull final Subscriber profile) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Either<Error, Subscriber> result = dao.createProfile(token.getName(), profile);

        return result.isRight()
                ? Response.status(Response.Status.CREATED)
                .entity(asJson(result.right().get()))
                .build()
                : Response.status(Response.Status.FORBIDDEN)
                .build();
    }

    @PUT
    @Consumes({"application/json"})
    public Response updateProfile(@Auth AccessTokenPrincipal token,
            @NotNull final Subscriber profile) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Either<Error, Subscriber> result = dao.updateProfile(token.getName(), profile);

        return result.isRight()
                ? Response.status(Response.Status.OK)
                .entity(asJson(result.right().get()))
                .build()
                : Response.status(Response.Status.NOT_FOUND)
                .build();
    }
}
