package org.ostelco.dropwizardutils

import com.fasterxml.jackson.annotation.JsonProperty
import org.eclipse.jetty.server.Authentication
import org.eclipse.jetty.server.UserIdentity
import java.io.IOException
import java.security.Principal
import java.security.cert.X509Certificate
import java.util.*
import javax.annotation.Priority
import javax.annotation.security.DenyAll
import javax.annotation.security.PermitAll
import javax.annotation.security.RolesAllowed
import javax.security.auth.Subject
import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.Priorities
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider


/**
 * A ContainerRequestFilter to do certificate validation beyond the tls validation.
 * For example, the filter matches the subject against a regex and will 403 if it doesn't match
 *
 *
 *
 * In
 * https://howtodoinjava.com/jersey/jersey-rest-security/
 * we can find an example of how to write an authentication filter
 * from scatch, that reacts to annotations, roles, this that
 * and misc. other things.  It is all good, but will have to wait
 * over the weekend.
 */

class CertConfig {
    // Userid, used in other parts of the permission system, e.g. when
    // assigning roles etc.
    @Valid
    @JsonProperty("userId")
    @NotNull
    var userId: String? = null

    // All the X.509 identifying fields
    //  And so on for all the X.509 fields
    //  C=NO; L=Fornebu; O=Open Source Telco; CN=smdpplus.ostelco.org
    @Valid
    @JsonProperty("country")
    @NotNull
    var country: String? = null

    @Valid
    @JsonProperty("state")
    @NotNull
    var state: String? = null

    @Valid
    @JsonProperty("location")
    @NotNull
    var location: String? = null

    @Valid
    @JsonProperty("organization")
    @NotNull
    var organization: String? = null

    @Valid
    @JsonProperty("commonName")
    @NotNull
    var commonName: String? = null

    @Valid
    @JsonProperty("roles")
    @NotNull
    var roles: MutableList<String> = mutableListOf()

}

class RolesConfig {
    @Valid
    @JsonProperty("definitions")
    @NotNull

    var roles: MutableList<RoleDef> = mutableListOf()
}

class RoleDef {
    @Valid
    @JsonProperty("name")
    @NotNull
    var name: String? = null

    @Valid
    @JsonProperty("description")
    @NotNull
    var description: String? = null

}

class CertAuthConfig {
    @Valid
    @JsonProperty("certAuths")
    @NotNull
    var certAuths = mutableListOf<CertConfig>()
}

/**
 * This filter verify the access permissions for a user
 * based on a client certificate provided when authenticating the
 * request.
 */
@Priority(Priorities.AUTHENTICATION)
@Provider
//@PreMatching // XXX Enable if possible
class CertificateAuthorizationFilter(private val rbac: RBACService) : javax.ws.rs.container.ContainerRequestFilter {

    @Context
    private var resourceInfo: ResourceInfo? = null

    // Although this is a class level field, Jersey actually injects a proxy
    // which is able to simultaneously serve more requests.
    @Context
    private var request: HttpServletRequest? = null

    private fun isUserAllowed(user: CertificateRBACUSER, rolesSet: Set<String>): Boolean {
        return rolesSet.intersect(user.roles.map{it.name}).isNotEmpty()
    }

    companion object {
        private val ACCESS_DENIED = Response.status(Response.Status.UNAUTHORIZED)
                .entity("You cannot access this resource").build()
        private val ACCESS_FORBIDDEN = Response.status(Response.Status.FORBIDDEN)
                .entity("Access blocked for all users !!").build()

        private const val X509_CERTIFICATE_ATTRIBUTE = "javax.servlet.request.X509Certificate"
    }

    //  XXX https://stackoverflow.com/questions/34654903/how-to-create-global-and-pre-post-matching-filter-in-restlet

    private fun certificateMatches(requestContext: ContainerRequestContext): CertificateRBACUSER? {


        val clientCert = extractClientCertFromRequest(requestContext) ?: return null

        val certParams = CertificateIdParameters.parse(clientCert)

        return rbac.findByCertParams(certParams)
    }

    private fun extractClientCertFromRequest(requestContext: ContainerRequestContext): X509Certificate? {
        val req = request

        if (req == null) {
            requestContext.abortWith(buildForbiddenResponse("No request found!"))
            return null
        }

        val certificatesUncast = req.getAttribute(X509_CERTIFICATE_ATTRIBUTE)
        if (certificatesUncast == null) {
            requestContext.abortWith(buildForbiddenResponse("No certificate chain found!"))
            return null
        }

        val certificateChain = certificatesUncast as Array<X509Certificate>


        if (certificateChain == null || certificateChain.isEmpty() || certificateChain[0] == null) {
            requestContext.abortWith(buildForbiddenResponse("No certificate chain found!"))
            return null
        }

        // The certificate of the client is always the first in the chain.

        return certificateChain[0]
    }

    @Throws(IOException::class)
    override fun filter(requestContext: ContainerRequestContext) {

        /* Fast exit if not called with https scheme.
            XXX: There must of course be a better way to do this, or? */

        if ("http" == requestContext.uriInfo.baseUri.scheme)
            return

        ///  IMPLEMENT FULL RBOC (with stubbed out permissiveness matrix).
        /// 1. Check certificate chain
        /// 2. Get user from certificate (using config read from config file, later from rboc server?)
        /// 3. From the user, and  set of permissions and annotations on resourdes,
        //     calculate if the user has permission to do what he/she wants to do with the
        //     resource.

        val user = certificateMatches(requestContext)

        if (user == null) {
            requestContext.abortWith(buildForbiddenResponse("Certificate subject is not recognized!"))
            return
        }

        val method = resourceInfo!!.resourceMethod
        //Access allowed for all
        if (method.isAnnotationPresent(PermitAll::class.java)) {
            return
        }
        //Access denied for all
        if (method.isAnnotationPresent(DenyAll::class.java)) {
            requestContext.abortWith(ACCESS_FORBIDDEN)
            return
        }

        //Verify user access
        if (method.isAnnotationPresent(RolesAllowed::class.java)) {
            val rolesAnnotation = method.getAnnotation(RolesAllowed::class.java)
            if (rolesAnnotation == null) {
                requestContext.abortWith(ACCESS_DENIED)
                return
            }
            val rolesSet = HashSet(Arrays.asList<String>(*rolesAnnotation.value))

            //Is user valid?
            if (!isUserAllowed(user, rolesSet)) {
                requestContext.abortWith(ACCESS_DENIED)
                return
            }
        }
    }

    private fun buildForbiddenResponse(message: String): Response {
        return Response.status(Response.Status.FORBIDDEN)
                .entity("{\"message\":\"$message\"}")
                .build()
    }
}

class RBACUserPrincipal(private val id: String) : Principal {
    override fun getName(): String {
        return id
    }
}


class RBACUserIdentity(id: String) : UserIdentity {

    private val principal: Principal
    private val mySubject: Subject

    init {
        this.principal = RBACUserPrincipal(id)
        this.mySubject = Subject()
    }

    override fun getSubject(): Subject {
        return this.mySubject
    }

    override fun isUserInRole(p0: String?, p1: UserIdentity.Scope?): Boolean {
        return false
    }

    override fun getUserPrincipal(): Principal {
        return principal
    }
}

/**
 * We're trying this out, not there yet.  The intent is to move towards a proper
 * RBAC system, so the role being referred to here is not really the same
 * as RBAC would assume.
 */
data class CertificateRBACUSER(
        val id: String,
        val roles: Set<RoleDef>,
        val commonName: String,
        val country: String,
        val state: String,
        val location: String,
        val organization: String) : Authentication.User {

    private val userId: UserIdentity

    init {
        userId = RBACUserIdentity(id)
    }

    override fun isUserInRole(p0: UserIdentity.Scope?, p1: String?): Boolean {
        return false
    }

    override fun getUserIdentity(): UserIdentity {
        return userId
    }

    override fun getAuthMethod(): String {
        return "CLIENT_CERTIFICATE"
    }

    override fun logout() {
        TODO("not implemented")
    }

    override fun logout(request: ServletRequest?): Authentication {
        TODO("not implemented")
    }

    fun asCertificateIdParamerters(): CertificateIdParameters {
        return CertificateIdParameters(country = country, state = state, location = location, organization = organization, commonName = commonName)
    }
}

class RBACService(val rolesConfig: RolesConfig, val certConfig: CertAuthConfig) {


    private val roles: MutableMap<String, RoleDef> = mutableMapOf()
    private val users: MutableMap<String, CertificateRBACUSER> = mutableMapOf()

    init {
        rolesConfig.roles.forEach {

            if (roles.putIfAbsent(it.name!!, it) != null) {
                throw RuntimeException("Multiple declarations of role ${it.name}")
            }
        }

        certConfig.certAuths.map {
            val user = certAuthToUser(it)
            users.put(user.id, user)
        }
    }

    private fun getRoleByName(name: String): RoleDef {
        if (!roles.containsKey(name)) {
            throw RuntimeException("Unknown role name $name")
        }

        return roles[name]!!
    }

    // R(val id: String, val commonName: String, val country: String, val state: String, val location: String, val organization: String) : Authentication.User {
    private fun certAuthToUser(cc: CertConfig): CertificateRBACUSER {


        val usersRoles = mutableSetOf<RoleDef>()

        cc.roles.forEach {
            if (roles.containsKey(it)) {
                usersRoles.add(roles[it]!!)
            } else {
                throw RuntimeException("User ${cc.userId} claims to have role $it, but it doesn't exist")
            }
        }


        return CertificateRBACUSER(id = cc.userId!!, roles = usersRoles, commonName = cc.commonName!!, country = cc.country!!, state = cc.state!!, location = cc.location!!, organization = cc.organization!!)
    }

    fun findByCertParams(certParams: CertificateIdParameters): CertificateRBACUSER? {
        return users.values.find {
            val cpm = it.asCertificateIdParamerters()

            val match = cpm == certParams
            match
        }
    }
}

// CN=*.not-really-ostelco.org, O=Not really SMDP org, L=Oslo, ST=Oslo, C=NO
data class CertificateIdParameters(val commonName: String, val country: String, val state: String, val location: String, val organization: String) {
    companion object {
        fun parse(cert: X509Certificate): CertificateIdParameters {

            val inputString= cert.subjectDN.name
            val parts = inputString.split(",")

            var countryName = ""
            var commonName  = ""
            var location  = ""
            var organization  = ""
            var state  = ""

            parts.forEach {
                val split = it.split("=")
                if (split.size != 2) {
                    throw RuntimeException("Illegal format for certificate")
                }
                val key = split[0].trim()
                val value = split[1].trim()



                when (key) {
                    "CN" -> commonName = value
                    "C" -> countryName = value
                    "OU" -> {
                    } // organizational unit
                    "O" -> organization = value
                    // organization
                    "L" -> location = value
                    // locality
                    "S" -> state = value
                    // XXX  State or province name
                    "ST" -> {
                    } //  State or province name
                } //  State or province name
            }

            return CertificateIdParameters(
                    commonName = commonName,
                    country = countryName,
                    location = location,
                    state = state,
                    organization = organization)
        }
    }
}


