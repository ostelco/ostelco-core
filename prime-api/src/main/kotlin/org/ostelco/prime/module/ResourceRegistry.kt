package org.ostelco.prime.module

import org.slf4j.LoggerFactory
import java.util.*

/**
 * Use this method to get implementation objects to interfaces in `prime-api` using {@link java.util.ServiceLoader}.
 * The libraries which have implementation classes should then add definition file to `META-INF/services`.
 * The name of the file should be name of Interface including package name.
 * The content of the file should be name of the implementing class including the package name.
 * Implementing class should have public no-args constructor.
 */
inline fun <reified T> getResource(): T {
    val services = ServiceLoader.load(T::class.java)
    val logger = LoggerFactory.getLogger(T::class.java)
    when (services.count()) {
        0 -> logger.error("No implementations found for interface ${T::class.simpleName}")
        1 -> {}
        else -> logger.warn("Multiple implementations found for interface ${T::class.simpleName}")
    }
    return services.first()
}