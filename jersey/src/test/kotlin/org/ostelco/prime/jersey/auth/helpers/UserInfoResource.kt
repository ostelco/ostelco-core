package org.ostelco.prime.jersey.auth.helpers

import javax.validation.Valid
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

/**
 * Fake OAuth2 'userinfo' endpoint for use in tests.
 *
 */
@Path("/userinfo")
class UserInfoResource {

    private val email = "boaty@internet.org"

    @GET
    @Produces("application/json")
    fun getUserInfo(@Valid @HeaderParam("Authorization") token: String?): Response {

        return if (token == null) {
            Response.status(Response.Status.NOT_FOUND)
                    .build()
        } else Response.status(Response.Status.OK)
                .entity("{\n" +
                        "    \"email\": \"" + email + "\"\n" +
                        "}\n")
                .build()

    }
}
