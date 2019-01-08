package org.ostelco.simcards.smdpplus

import com.fasterxml.jackson.annotation.JsonProperty
import org.eclipse.jetty.server.Authentication
import org.eclipse.jetty.server.UserIdentity
import java.io.IOException
import java.security.Principal
import java.security.cert.X509Certificate
import java.util.*
import java.util.regex.Pattern
import javax.annotation.Priority
import javax.annotation.security.DenyAll
import javax.annotation.security.PermitAll
import javax.annotation.security.RolesAllowed
import javax.security.auth.Subject
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
    var roles: MutableList<String> = mutableListOf<String>()

}

class RolesConfig {
    @Valid
    @JsonProperty("definitions")
    @NotNull
    var roles: MutableList<RoleDef> = mutableListOf<RoleDef>()

}


class RoleDef {
    @Valid
    @JsonProperty("name")
    @NotNull
    var name:String? = null

    @Valid
    @JsonProperty("description")
    @NotNull
    var description:String? = null

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
class CertificateAuthorizationFilter (rbac: RBACService): javax.ws.rs.container.ContainerRequestFilter {

    @Context
    private var resourceInfo: ResourceInfo? = null

    // Although this is a class level field, Jersey actually injects a proxy
    // which is able to simultaneously serve more requests.
    @Context
    private var request: HttpServletRequest? = null

    private fun isUserAllowed(username: CertificateRBACUSER, rolesSet: Set<String>): Boolean {
        var isAllowed = false

        // XXX check this a little more thoroughly
        return true
    }

    companion object {
        private val ACCESS_DENIED = Response.status(Response.Status.UNAUTHORIZED)
                .entity("You cannot access this resource").build()
        private val ACCESS_FORBIDDEN = Response.status(Response.Status.FORBIDDEN)
                .entity("Access blocked for all users !!").build()
        private val X509_CERTIFICATE_ATTRIBUTE = "javax.servlet.request.X509Certificate"
    }

    //  XXX https://stackoverflow.com/questions/34654903/how-to-create-global-and-pre-post-matching-filter-in-restlet
    private val dnRegex: Pattern = Pattern.compile(".*") // XXX Maximally permissive

    private fun certificateMatches(requestContext: ContainerRequestContext): CertificateRBACUSER? {

        val req = request

        if (req == null) {
            requestContext.abortWith(buildForbiddenResponse("No request found!"))
            return null
        }

        val certificatesUncast = req.getAttribute(X509_CERTIFICATE_ATTRIBUTE)
        if (certificatesUncast == null){
            requestContext.abortWith(buildForbiddenResponse("No certificate chain found!"))
            return null
        }

        val certificateChain = certificatesUncast as Array<X509Certificate>

        if (certificateChain == null || certificateChain.size == 0 || certificateChain[0] == null) {
            requestContext.abortWith(buildForbiddenResponse("No certificate chain found!"))
            return null
        }


        // The certificate of the client is always the first in the chain.
        val clientCert = certificateChain[0]
        val clientCertDN = clientCert.subjectDN.name

        // XXX Don't use regexp matching.  Parse the clientCertDN into constituents, and then require
        //     perfect match, and based on the matching patterh infer
        //     which party is using the certificate to authenticate.  It might also be a decent idea
        //     to require a fingerprint match of the cert.
        dnRegex.matcher(clientCertDN).matches()

        // XXX Placeholder, should do lookup in user registry
        return CertificateRBACUSER("foo", "bar", "baz", "gazonk", "foo")  // XXX should use loookup of the certificate instead of a static string
    }


    @Throws(IOException::class)
    override fun filter(requestContext: ContainerRequestContext) {

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


class RBACUserPrincipal(val id: String): Principal {
    override fun getName(): String {
        return id
    }
}
class RBACUserIdentity(val id:String) : UserIdentity {

    val principal: Principal
    val mySubject: Subject


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
data class CertificateRBACUSER (val id: String, val commonName: String, val country: String, val location: String, val organization: String) : Authentication.User {

    val userId: UserIdentity
    init {
        userId =  RBACUserIdentity(id)
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
}


class RBACService(val rolesConfig: RolesConfig,  val certConfig:CertAuthConfig) {

    val roles : MutableMap<String, RoleDef>  = mutableMapOf<String, RoleDef>()
    val users : MutableMap<String, CertificateRBACUSER> = mutableMapOf()

    init {
        rolesConfig.roles.forEach{
            if (roles.putIfAbsent(it.name!!, it!!) != null) {
                throw RuntimeException("Multiple declarations of role ${it.name}")
            }
        }

         certConfig.certAuths.map {
             val user = certAuthToUser(it)
             users.put(user.id, user)
         }
    }

    private fun getRoleByName(name: String) : RoleDef {
        if (!roles.containsKey(name)) {
            throw RuntimeException("Unknown role name $name")
        }
        return roles.get(name)!!
    }

    private fun certAuthToUser(cc: CertConfig) : CertificateRBACUSER {
        return CertificateRBACUSER(cc.userId!!, cc.commonName!!, cc.country!!, cc.location!!, cc.organization!!)
    }
}


