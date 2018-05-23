package org.ostelco.topup.api.auth.helpers;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

/**
 * Fake OAuth2 'userinfo' endpoint for use in tests.
 *
 */
@Path("/userinfo")
public class UserInfoResource {

    private final String email = "boaty@internet.org";

    @GET
    @Produces({"application/json"})
    public Response getuserInfo(@Valid @HeaderParam("Authorization") String token) {

        if (token == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .build();
        }

        return Response.status(Response.Status.OK)
            .entity("{\n" +
                    "    \"email\": \"" + email + "\"\n" +
                    "}\n")
            .build();
    }
}
