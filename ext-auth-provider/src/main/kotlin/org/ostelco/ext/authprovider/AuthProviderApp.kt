package org.ostelco.ext.authprovider

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import javax.validation.Valid
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

fun main(args: Array<String>) {
    AuthProviderApp().run("server")
}

class AuthProviderApp : Application<Configuration>() {

    override fun run(
            config: Configuration,
            env: Environment) {

        env.jersey().register(UserInfoResource())
    }
}

@Path("/userinfo")
class UserInfoResource {

    private val email = "foo@bar.com"

    @GET
    @Produces("application/json")
    fun getuserInfo(@Valid @HeaderParam("Authorization") token: String?): Response {

        return if (token == null) {
            Response.status(Response.Status.NOT_FOUND).build()
        } else Response.status(Response.Status.OK)
                .entity("""{ "email": "$email" }""")
                .build()
    }
}