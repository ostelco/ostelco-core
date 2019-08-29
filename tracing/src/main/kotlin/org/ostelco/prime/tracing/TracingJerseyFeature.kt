package org.ostelco.prime.tracing

import io.opencensus.common.Scope
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.container.DynamicFeature
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.FeatureContext

/**
 * Dynamic feature which will be applied on JAX-RS resource methods if they are annotated with [EnableTracing] annotation.
 * This feature applies [TracingFilter] on those annotated methods.
 *
 */
class TracingFeature : DynamicFeature {

    override fun configure(
            resourceInfo: ResourceInfo?,
            context: FeatureContext?) {

        if (resourceInfo?.resourceMethod?.getAnnotation(EnableTracing::class.java) != null) {
            context?.register(TracingFilter::class.java)
        }
    }
}

/**
 * Filter which start and stop a trace scan for JAX-RS resource methods.
 */
class TracingFilter : ContainerRequestFilter, ContainerResponseFilter {

    override fun filter(requestContext: ContainerRequestContext?) {
        val scope = TraceSingleton.createScopedSpan("%s /%s".format(requestContext?.method, requestContext?.uriInfo?.path))
        requestContext?.setProperty(TRACE_SCOPE, scope)
    }

    override fun filter(
            requestContext: ContainerRequestContext?,
            responseContext: ContainerResponseContext?) {

        val scope = requestContext?.getProperty(TRACE_SCOPE) as Scope?
        scope?.close()
    }

    companion object {
        const val TRACE_SCOPE = "TRACE_SCOPE"
    }
}