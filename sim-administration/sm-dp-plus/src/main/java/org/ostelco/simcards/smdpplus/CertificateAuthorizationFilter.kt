package org.ostelco.simcards.smdpplus

import org.glassfish.jersey.internal.util.Base64

import javax.annotation.security.DenyAll
import javax.annotation.security.PermitAll
import javax.annotation.security.RolesAllowed
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.Context
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider
import java.lang.reflect.Method
import java.util.*

/**
 * This filter verify the access permissions for a user
 * based on a client certificate provided when authenticating the
 * request.
 */
@Provider
class CertificateAuthorizationFilter : javax.ws.rs.container.ContainerRequestFilter {

    @Context
    private val resourceInfo: ResourceInfo? = null

    override fun filter(requestContext: ContainerRequestContext) {
        val method = resourceInfo!!.resourceMethod
        //Access allowed for all
        if (!method.isAnnotationPresent(PermitAll::class.java)) {
            //Access denied for all
            if (method.isAnnotationPresent(DenyAll::class.java)) {
                requestContext.abortWith(ACCESS_FORBIDDEN)
                return
            }

            //Get request headers
            val headers = requestContext.headers

            //Fetch authorization header
            val authorization = headers[AUTHORIZATION_PROPERTY]

            //If no authorization information present; block access
            if (authorization == null || authorization.isEmpty()) {
                requestContext.abortWith(ACCESS_DENIED)
                return
            }

            //Get encoded username and password
            val encodedUserPassword = authorization[0].replaceFirst("$AUTHENTICATION_SCHEME ".toRegex(), "")

            //Decode username and password
            val usernameAndPassword = String(Base64.decode(encodedUserPassword.toByteArray()))

            //Split username and password tokens
            val tokenizer = StringTokenizer(usernameAndPassword, ":")
            val username = tokenizer.nextToken()
            val password = tokenizer.nextToken()

            //Verifying Username and password
            println(username)
            println(password)

            //Verify user access
            if (method.isAnnotationPresent(RolesAllowed::class.java)) {
                val rolesAnnotation = method.getAnnotation(RolesAllowed::class.java)
                val rolesSet = HashSet(Arrays.asList<String>(*rolesAnnotation.value()))

                //Is user valid?
                if (!isUserAllowed(username, password, rolesSet)) {
                    requestContext.abortWith(ACCESS_DENIED)
                    return
                }
            }
        }
    }

    private fun isUserAllowed(username: String, password: String, rolesSet: Set<String>): Boolean {
        var isAllowed = false

        //Step 1. Fetch password from database and match with password in argument
        //If both match then get the defined role for user from database and continue; else return isAllowed [false]
        //Access the database and do this part yourself
        //String userRole = userMgr.getUserRole(username);

        if (username == "howtodoinjava" && password == "password") {
            val userRole = "ADMIN"

            //Step 2. Verify user role
            if (rolesSet.contains(userRole)) {
                isAllowed = true
            }
        }
        return isAllowed
    }

    companion object {

        private val AUTHORIZATION_PROPERTY = "Authorization"
        private val AUTHENTICATION_SCHEME = "Basic"
        private val ACCESS_DENIED = Response.status(Response.Status.UNAUTHORIZED)
                .entity("You cannot access this resource").build()
        private val ACCESS_FORBIDDEN = Response.status(Response.Status.FORBIDDEN)
                .entity("Access blocked for all users !!").build()
    }
}