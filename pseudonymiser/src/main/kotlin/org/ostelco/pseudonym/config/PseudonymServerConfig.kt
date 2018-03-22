package org.ostelco.pseudonym.config

import io.dropwizard.Configuration

class PseudonymServerConfig : Configuration() {

    var serviceAccountKey = ""

    var databaseName = ""
}
