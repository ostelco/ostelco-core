package org.ostelco.pseudonymiser.config

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Configuration
import io.dropwizard.client.JerseyClientConfiguration
import javax.validation.Valid



/**
 * The configuration for Pseudonymiser.
 */
class PseudonymiserConfig : Configuration() {

    var serviceAccountKey = ""
    var projectName = "pantel-2decb"
    var subscriptionName = "test-pseudo"
    var publisherTopic = "pseudo-traffic"
    var pseudonymEndpoint = ""
    var jerseyClient = JerseyClientConfiguration()
}
