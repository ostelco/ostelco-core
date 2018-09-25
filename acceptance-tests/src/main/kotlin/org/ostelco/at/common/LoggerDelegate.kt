package org.ostelco.at.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This is a function to which the member variable of type [org.slf4j.Logger] is delegated to be instantiated.
 * The syntax to do so is <code>private val logger by getLogger()</code>.
 * This function will then return the [org.slf4j.Logger] for calling class.
 */
fun <R : Any> R.getLogger(): Lazy<Logger> = lazy {
    LoggerFactory.getLogger(this.javaClass)
}
