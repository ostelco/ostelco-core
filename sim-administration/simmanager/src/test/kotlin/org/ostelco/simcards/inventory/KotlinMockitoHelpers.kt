package org.ostelco.simcards.inventory

import org.mockito.Mockito


/**
 * Helper class for making type-safe mocks in Kotlin.
 */
class KotlinMockitoHelpers  {
    companion object {
        /**
         * Generic, but statically typed "any" matcher for use in Mockito mocks.
         */
        fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

        /**
         * Generic, but statically typed "eq" matcher for use in Mockito mocks.
         */
        fun <T> eq(obj: T): T = Mockito.eq<T>(obj)
    }
}