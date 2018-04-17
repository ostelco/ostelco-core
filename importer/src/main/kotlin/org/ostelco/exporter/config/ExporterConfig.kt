package org.ostelco.importer.config

import io.dropwizard.Configuration
import io.dropwizard.client.JerseyClientConfiguration

/**
 * The configuration for Importer.
 */
class ImporterConfig : Configuration() {

    var serviceAccountKey = ""
    var databaseName = ""
    var datastoreType = "default"
    var projectName = "pantel-2decb"
    var subscriptionName = "test-pseudo"
    var publisherTopic = "pseudo-traffic"
    var pseudonymEndpoint = "http://pseudonym-server-service:80"
    var jerseyClient = JerseyClientConfiguration()
}
