package org.ostelco.prime.provider

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import io.dropwizard.jackson.Discoverable
import io.dropwizard.setup.Environment

/**
 * Prime is a multi-provider component, wherein each component is a separate library.
 * Provider is such a library which needs access to the Dropwizard's {@link io.dropwizard.setup.Environment}
 * for actions like registering {@link io.dropwizard.lifecycle.Managed} objects, `Resources`,
 * {@link com.codahale.metrics.health.HealthCheck} etc.
 * Each library to be a valid provider has to implement this interface.
 * That class will then get {@link io.dropwizard.setup.Environment} object in the init method which is overridden.
 *
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
interface Service : Discoverable {
    fun init(env: Environment) {}
}