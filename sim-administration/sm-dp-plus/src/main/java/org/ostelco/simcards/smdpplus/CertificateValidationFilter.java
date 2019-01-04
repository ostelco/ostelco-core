package org.ostelco.simcards.smdpplus;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

/**
 * A ContainerRequestFilter to do certificate validation beyond the tls validation.
 * For example, the filter matches the subject against a regex and will 403 if it doesn't match
 *
 * @author <a href="mailto:wdawson@okta.com">wdawson</a>
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public final class CertificateValidationFilter implements ContainerRequestFilter {

    private static final String X509_CERTIFICATE_ATTRIBUTE = "javax.servlet.request.X509Certificate";

    private final Pattern dnRegex;

    // Although this is a class level field, Jersey actually injects a proxy
    // which is able to simultaneously serve more requests.
    @Context
    private HttpServletRequest request;

    /**
     * Constructor for the CertificateValidationFilter.
     *
     * @param dnRegex The regular expression to match subjects of certificates with.
     *                E.g.: "^CN=service1\.example\.com$"
     */
    public CertificateValidationFilter(String dnRegex) {
        this.dnRegex = Pattern.compile(dnRegex);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        X509Certificate[] certificateChain = (X509Certificate[]) request.getAttribute(X509_CERTIFICATE_ATTRIBUTE);

        if (certificateChain == null || certificateChain.length == 0 || certificateChain[0] == null) {
            requestContext.abortWith(buildForbiddenResponse("No certificate chain found!"));
            return;
        }

        // The certificate of the client is always the first in the chain.
        X509Certificate clientCert = certificateChain[0];
        String clientCertDN = clientCert.getSubjectDN().getName();

        if (!dnRegex.matcher(clientCertDN).matches()) {
            requestContext.abortWith(buildForbiddenResponse("Certificate subject is not recognized!"));
        }
    }

    private Response buildForbiddenResponse(String message) {
        return Response.status(Response.Status.FORBIDDEN)
                .entity("{\"message\":\"" + message + "\"}")
                .build();
    }
}