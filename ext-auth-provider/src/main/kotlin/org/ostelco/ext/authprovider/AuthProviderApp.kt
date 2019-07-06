package org.ostelco.ext.authprovider

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import javax.validation.Valid
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response

internal val JWT_SIGNING_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS512)

fun main() = AuthProviderApp().run("server")

class AuthProviderApp : Application<Configuration>() {

    override fun run(
            config: Configuration,
            env: Environment) {

        env.jersey().register(UserInfoResource())
    }
}

@Path("/userinfo")
class UserInfoResource {

    @GET
    @Produces("application/json")
    fun getUserInfo(@Valid @HeaderParam("Authorization") token: String?): Response {

        if (token != null) {

            val claims = Jwts.parser()
                    .setSigningKey(JWT_SIGNING_KEY)
                    .parseClaimsJws(token.removePrefix("Bearer "))
                    .body

            return Response.status(Response.Status.OK)
                    .entity("""{ "email": "${claims.subject}" }""")
                    .build()
        }
        return Response.status(Response.Status.NOT_FOUND).build()
    }
}

