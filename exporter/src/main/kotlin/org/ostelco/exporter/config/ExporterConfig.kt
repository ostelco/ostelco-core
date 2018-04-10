package org.ostelco.exporter.config

import io.dropwizard.Configuration
import io.dropwizard.client.JerseyClientConfiguration

/**
 * The configuration for Exporter.
 */
class ExporterConfig : Configuration() {

    var serviceAccountKey = ""
    var databaseName = ""
    var datastoreType = "default"
    var projectName = "pantel-2decb"
    var subscriptionName = "test-pseudo"
    var publisherTopic = "pseudo-traffic"
    var pseudonymEndpoint = ""
    var jerseyClient = JerseyClientConfiguration()
}
