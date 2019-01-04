package org.ostelco.simcards.smdpplus

import java.io.IOException
import java.security.cert.X509Certificate
import java.util.regex.Pattern
import javax.annotation.Priority
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Priorities
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.PreMatching
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response

/**
 * A ContainerRequestFilter to do certificate validation beyond the tls validation.
 * For example, the filter matches the subject against a regex and will 403 if it doesn't match
 *
 *
 * In
 * https://howtodoinjava.com/jersey/jersey-rest-security/
 * we can find an example of how to write an authentication filter
 * from scatch, that reacts to annotations, roles, this that
 * and misc. other things.  It is all good, but will have to wait
 * over the weekend.
 *
 *
 * @author [wdawson](mailto:wdawson@okta.com)
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
class CertificateValidationFilter
/**
 * Constructor for the CertificateValidationFilter.
 *
 * @param dnRegex The regular expression to match subjects of certificates with.
 * E.g.: "^CN=service1\.example\.com$"
 */
(dnRegex: String) : ContainerRequestFilter {

    private val dnRegex: Pattern

    // Although this is a class level field, Jersey actually injects a proxy
    // which is able to simultaneously serve more requests.
    @Context
    private val request: HttpServletRequest? = null

    init {
        this.dnRegex = Pattern.compile(dnRegex)
    }

    @Throws(IOException::class)
    override fun filter(requestContext: ContainerRequestContext) {
        val certificateChain = request!!.getAttribute(X509_CERTIFICATE_ATTRIBUTE) as Array<X509Certificate>

        if (certificateChain == null || certificateChain.size == 0 || certificateChain[0] == null) {
            requestContext.abortWith(buildForbiddenResponse("No certificate chain found!"))
            return
        }

        // The certificate of the client is always the first in the chain.
        val clientCert = certificateChain[0]
        val clientCertDN = clientCert.subjectDN.name

        if (!dnRegex.matcher(clientCertDN).matches()) {
            requestContext.abortWith(buildForbiddenResponse("Certificate subject is not recognized!"))
        }
    }

    private fun buildForbiddenResponse(message: String): Response {
        return Response.status(Response.Status.FORBIDDEN)
                .entity("{\"message\":\"$message\"}")
                .build()
    }

    companion object {

        private val X509_CERTIFICATE_ATTRIBUTE = "javax.servlet.request.X509Certificate"
    }
}