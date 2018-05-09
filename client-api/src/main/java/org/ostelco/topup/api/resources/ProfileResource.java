package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.auth.AccessTokenPrincipal;
import org.ostelco.topup.api.core.EndpointUserInfo;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.core.Profile;
import org.ostelco.topup.api.db.SubscriberDAO;

import io.dropwizard.auth.Auth;
import io.vavr.control.Either;
import io.vavr.control.Option;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Profile API.
 *
 */
@AllArgsConstructor
@Path("/profile")
public class ProfileResource extends ResourceHelpers {

    @NonNull
    private final SubscriberDAO dao;

    @GET
    @Produces({"application/json"})
    public Response getProfile(@Auth AccessTokenPrincipal token,
            @Valid @HeaderParam("X-Endpoint-API-UserInfo") EndpointUserInfo userInfo) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Either<Error, Profile> result = dao.getProfile(token.getName());

        return result.isRight()
            ? Response.status(Response.Status.OK)
                 .entity(getProfileAsJson(result.right().get()))
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .build();
    }

    @PUT
    public Response updateProfile(@Auth AccessTokenPrincipal token,
            @Valid @HeaderParam("X-Endpoint-API-UserInfo") EndpointUserInfo userInfo,
            final Profile profile) {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .build();
        }

        Option<Error> error = dao.updateProfile(token.getName(), profile);

        return error.isEmpty()
            ? Response.status(Response.Status.OK)
                 .build()
            : Response.status(Response.Status.NOT_FOUND)
                 .entity(getErrorAsJson(error.get()))
                 .build();
    }
}
