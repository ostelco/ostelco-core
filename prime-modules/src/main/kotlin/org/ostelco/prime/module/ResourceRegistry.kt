package org.ostelco.prime.module

import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Named

/**
 * Use this method to get implementation objects to interfaces in `prime-modules` using [java.util.ServiceLoader].
 * The libraries which have implementation classes should then add definition file to `META-INF/services`.
 * The name of the file should be name of Interface including package name.
 * The content of the file should be name of the implementing class including the package name.
 * Implementing class should have public no-args constructor.
 */
inline fun <reified T: Any> getResource(name: String? = null): T {
    val services = ServiceLoader.load(T::class.java)
    val logger = LoggerFactory.getLogger(T::class.java)
    val providers = if (name != null) {
        services.filter { provider:T ->
            provider::class.java.isAnnotationPresent(Named::class.java)
                    && provider::class.java.getAnnotation(Named::class.java).value == name
        }
    } else {
        services.toList()
    }
    return when (providers.count()) {
        0 -> throw Exception("No implementations ${name?.let { "named $it" } ?: ""} found for interface ${T::class.simpleName}")
        1 -> providers.first()
        else -> {
            logger.warn("Multiple implementations ${name?.let { "named $it" } ?: ""} found for interface ${T::class.simpleName}")
            providers.first()
        }
    }
}