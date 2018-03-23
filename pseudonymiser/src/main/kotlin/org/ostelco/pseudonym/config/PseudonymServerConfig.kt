package org.ostelco.pseudonym.config

import io.dropwizard.Configuration

/**
 * The configuration for Pseudonymiser.
 */
class PseudonymServerConfig : Configuration() {

    var serviceAccountKey = ""

    var databaseName = ""
}
