package org.ostelco.auth.config

import io.dropwizard.Configuration

class AuthServerConfig : Configuration() {

    var serviceAccountKey = ""

    var databaseName = ""
}
