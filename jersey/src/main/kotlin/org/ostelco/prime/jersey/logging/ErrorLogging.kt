package org.ostelco.prime.jersey.logging

import org.ostelco.prime.getLogger
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.container.DynamicFeature
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.FeatureContext

/**
 * Dynamic feature which will be applied on JAX-RS resource methods if they are annotated with [Critical] annotation.
 * This feature applies [ErrorLoggingFilter] on those annotated methods.
 *
 */
class ErrorLoggingFeature : DynamicFeature {

    override fun configure(
            resourceInfo: ResourceInfo?,
            context: FeatureContext?) {

        if (resourceInfo?.resourceMethod?.getAnnotation(Critical::class.java) != null) {
            context?.register(ErrorLoggingFilter::class.java)
        }
    }
}

/**
 * Filter which will log non-2xx responses with ERROR level and NOTIFY_OPS marker.
 */
class ErrorLoggingFilter : ContainerResponseFilter {

    private val logger by getLogger()

    override fun filter(
            requestContext: ContainerRequestContext?,
            responseContext: ContainerResponseContext?) {

        if (responseContext?.status !in 200..299) {

            logger.error(
                    NOTIFY_OPS_MARKER,
                    "{} /{} - {} : {}",
                    requestContext?.method,
                    requestContext?.uriInfo?.path,
                    responseContext?.status,
                    responseContext?.entity
            )
        }
    }
}