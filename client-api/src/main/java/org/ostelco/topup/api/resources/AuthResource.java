package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.core.Grant;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.db.SubscriberDAO;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import io.vavr.control.Either;

/**
 * Sign up API.
 *
 */
@AllArgsConstructor
@Path("/auth/token")
public class AuthResource extends ResourceHelpers {

    @NonNull
    private final SubscriberDAO dao;

    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public Response authenticate(final Grant grant) {

        Either<Error, String> result = dao.handleGrant(grant);

        return result.isRight()
            ? Response.ok()
                 .header("Cache-Control", "no-store")
                 .header("Pragma", "no-cache")
                 .entity(result.right().get())
                 .build()
            : Response.status(Response.Status.FORBIDDEN)
                 .build();
    }
}
