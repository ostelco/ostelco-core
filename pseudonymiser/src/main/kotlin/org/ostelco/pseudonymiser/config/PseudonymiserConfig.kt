package org.ostelco.pseudonymiser.config

import io.dropwizard.Configuration

/**
 * The configuration for Pseudonymiser.
 */
class PseudonymiserConfig : Configuration() {

    var serviceAccountKey = ""
    var databaseName = ""
    var datastoreType = "default"
}
