package org.ostelco.prime.logging

import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import org.slf4j.MDC
import java.util.UUID
import javax.ws.rs.ext.Provider

/**
 * Add an unique id to each request simplyfying tracking of requests in logs.
 */
@Provider
class TrackRequestsLoggingFilter : ContainerRequestFilter, ContainerResponseFilter {

    /* Commonly used HTTP header for tracing requests. */
    val REQUEST_TRACE_HEADER = "X-RequestTrace"

    /* MDC tracking. */
    val TRACE_ID = "TraceId"

    override fun filter(ctx: ContainerRequestContext) {
        val traceHeader = ctx.getHeaderString(REQUEST_TRACE_HEADER)
        MDC.put("InvocationId", if (!traceHeader.isNullOrBlank())
            traceHeader
        else
            UUID.randomUUID().toString())
    }

    override fun filter(reqCtx: ContainerRequestContext, rspCtx: ContainerResponseContext) {
        MDC.remove(TRACE_ID)
    }
}
