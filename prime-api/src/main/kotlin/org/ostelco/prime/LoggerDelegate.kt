package org.ostelco.prime

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This is a function to which the member variable of type {@link org.slf4j.Logger} is delegated to be instantiated.
 * The syntax to do so is `private val logger by logger()`.
 * This function will then return the logger for calling class.
 */
fun <R : Any> R.logger(): Lazy<Logger> = lazy {
    LoggerFactory.getLogger(this.javaClass)
}
