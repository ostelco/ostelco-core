package org.ostelco.prime.jersey.logging

import org.apache.commons.codec.digest.DigestUtils
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.slf4j.MDC
import java.security.Principal
import java.util.*
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter

class IdentityLoggingJaxRsFilter : ContainerRequestFilter, ContainerResponseFilter {

    override fun filter(ctx: ContainerRequestContext) {
        val userPrincipal: Principal? = ctx.securityContext.userPrincipal
        if (userPrincipal is AccessTokenPrincipal) {
            val idSha256 = String(Base64.getEncoder().encode(DigestUtils.sha256(userPrincipal.identity.id)))
            MDC.put(ID_KEY, idSha256)
        }
    }

    override fun filter(
            reqCtx: ContainerRequestContext,
            respCtx: ContainerResponseContext) {

        MDC.remove(ID_KEY)
    }

    companion object {
        private const val ID_KEY = "customerIdentity"
    }
}