package org.ostelco.prime.jersey.logging

import org.slf4j.MDC
import java.util.*
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.ext.Provider

/**
 * Add an unique id to each request simplyfying tracking of requests in logs.
 */
@Provider
class TrackRequestsLoggingJaxRsFilter : ContainerRequestFilter, ContainerResponseFilter {

    /* Commonly used HTTP header for tracing requests. */
    private val requestTraceHeader = "X-Request-ID"

    /* MDC tracking. */
    private val traceId = "TraceId"

    override fun filter(ctx: ContainerRequestContext) {
        val traceHeader = ctx.getHeaderString(requestTraceHeader)
        MDC.put(traceId,
                if (!traceHeader.isNullOrBlank())
                    traceHeader
                else
                    UUID.randomUUID().toString())
    }

    override fun filter(reqCtx: ContainerRequestContext, rspCtx: ContainerResponseContext) {
        MDC.remove(traceId)
    }
}
