package org.ostelco.topup.api.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ostelco.prime.client.api.model.Consent;
import org.ostelco.prime.client.api.model.Product;
import org.ostelco.prime.client.api.model.Profile;
import org.ostelco.prime.client.api.model.SubscriptionStatus;
import org.ostelco.topup.api.core.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Common 'helper' functions for resources.
 *
 */
public abstract class ResourceHelpers {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceHelpers.class);

    private final ObjectMapper MAPPER = new ObjectMapper();

    /* Profile POJO to JSON. */
    protected String getProfileAsJson(final Profile profile) {
        try {
            return MAPPER.writeValueAsString(profile);
        } catch (JsonProcessingException e) {
            LOG.error("Error in json response {}", e);
        }
        return "";
    }

    /* Subscription POJO to JSON. */
    protected String getSubscriptionStatusAsJson(final SubscriptionStatus subscriptionStatus) {
        try {
            return MAPPER.writeValueAsString(subscriptionStatus);
        } catch (JsonProcessingException e) {
            LOG.error("Error in json response {}", e);
        }
        return "";
    }

    /* Product POJO to JSON. */
    protected String getProductsAsJson(final List<Product> products) {
        try {
            return MAPPER.writeValueAsString(products);
        } catch (JsonProcessingException e) {
            LOG.error("Error in json response {}", e);
        }
        return "";
    }

    /* Consent POJO to JSON. */
    protected String getConsentsAsJson(final List<Consent> consents) {
        try {
            return MAPPER.writeValueAsString(consents);
        } catch (JsonProcessingException e) {
            LOG.error("Error in json response {}", e);
        }
        return "";
    }

    /* Error POJO to JSON. */
    protected String getErrorAsJson(final Error error) {
        try {
            return MAPPER.writeValueAsString(error);
        } catch (JsonProcessingException e) {
            LOG.error("Error in json response {}", e);
        }
        return "";
    }
}
