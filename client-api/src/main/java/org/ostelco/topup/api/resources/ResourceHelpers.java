package org.ostelco.topup.api.resources;

import org.ostelco.topup.api.core.Consent;
import org.ostelco.topup.api.core.Error;
import org.ostelco.topup.api.core.Offer;
import org.ostelco.topup.api.core.Profile;
import org.ostelco.topup.api.core.SubscriptionStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /* Offers POJO to JSON. */
    protected String getOffersAsJson(final List<Offer> offers) {
        try {
            return MAPPER.writeValueAsString(offers);
        } catch (JsonProcessingException e) {
            LOG.error("Error in json response {}", e);
        }
        return "";
    }

    /* Consents POJO to JSON. */
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
