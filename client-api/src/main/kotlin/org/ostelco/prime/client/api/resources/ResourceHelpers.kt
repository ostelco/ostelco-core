package org.ostelco.prime.client.api.resources

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.ostelco.prime.logger

/**
 * Common 'helper' functions for resources.
 *
 */
abstract class ResourceHelpers {

    private val LOG by logger()

    private val MAPPER = ObjectMapper()

    protected fun asJson(`object`: Any): String {
        try {
            return MAPPER.writeValueAsString(`object`)
        } catch (e: JsonProcessingException) {
            LOG.error("Error in json response {}", e)
        }
        return ""
    }
}
