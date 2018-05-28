package org.ostelco.topup.api.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common 'helper' functions for resources.
 *
 */
public abstract class ResourceHelpers {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceHelpers.class);

    private final ObjectMapper MAPPER = new ObjectMapper();

    protected String asJson(final Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOG.error("Error in json response {}", e);
        }
        return "";
    }
}
