package org.ostelco.pseudonym.config

import io.dropwizard.Configuration
import io.dropwizard.client.JerseyClientConfiguration

/**
 * The configuration for Pseudonymiser.
 */
class PseudonymServerConfig : Configuration() {

    var serviceAccountKey = ""
    var databaseName = ""
    var datastoreType = "default"
    var projectName = "pantel-2decb"
    var subscriptionName = "test-pseudo"
    var publisherTopic = "pseudo-traffic"
    var pseudonymEndpoint = ""
    var jerseyClient = JerseyClientConfiguration()
}
