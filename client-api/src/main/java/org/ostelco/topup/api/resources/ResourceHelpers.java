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

//    /* Profile POJO to JSON. */
//    protected String getProfileAsJson(final Subscriber profile) {
//        try {
//            return MAPPER.writeValueAsString(profile);
//        } catch (JsonProcessingException e) {
//            LOG.error("Error in json response {}", e);
//        }
//        return "";
//    }
//
//    /* Subscription POJO to JSON. */
//    protected String getSubscriptionStatusAsJson(final SubscriptionStatus subscriptionStatus) {
//        try {
//            return MAPPER.writeValueAsString(subscriptionStatus);
//        } catch (JsonProcessingException e) {
//            LOG.error("Error in json response {}", e);
//        }
//        return "";
//    }
//
//    /* Product POJO to JSON. */
//    protected String getProductsAsJson(final Collection<Product> products) {
//        try {
//            return MAPPER.writeValueAsString(products);
//        } catch (JsonProcessingException e) {
//            LOG.error("Error in json response {}", e);
//        }
//        return "";
//    }
//
//    /* Consent POJO to JSON. */
//    protected String getConsentsAsJson(final Collection<Consent> consents) {
//        try {
//            return MAPPER.writeValueAsString(consents);
//        } catch (JsonProcessingException e) {
//            LOG.error("Error in json response {}", e);
//        }
//        return "";
//    }
//
//    /* Error POJO to JSON. */
//    protected String getErrorAsJson(final Error error) {
//        try {
//            return MAPPER.writeValueAsString(error);
//        } catch (JsonProcessingException e) {
//            LOG.error("Error in json response {}", e);
//        }
//        return "";
//    }
}
