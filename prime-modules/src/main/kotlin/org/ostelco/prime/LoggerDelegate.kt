package org.ostelco.prime

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This is a function to which the member variable of type [org.slf4j.Logger] is delegated to be instantiated.
 * The syntax to do so is `private val getLogger by getLogger()`.
 * This function will then return the getLogger for calling class.
 */
fun <R : Any> R.getLogger(): Lazy<Logger> = lazy {
    LoggerFactory.getLogger(this.javaClass)
}
