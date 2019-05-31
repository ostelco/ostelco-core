package org.ostelco.tools.prime.admin

import org.ostelco.prime.PrimeApplication
import org.ostelco.tools.prime.admin.modules.DwEnvModule
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Update `config/config.yaml` to point to valid Neo4j store.
 * For GCP K8s setup, this means setting up port-forwarding which is documented in `/docs/NEO4J.md`.
 */
fun main() {
    PrimeApplication().run("server", "config/config.yaml")
    try {
        doActions()
    } finally {
        DwEnvModule.env.applicationContext.shutdown().get(10, SECONDS)
    }
}

fun doActions() {

    // createCustomer(email = "", nickname = "")

    // link to region

    // add SimProfile

    // remove SimProfile

    // set bundle balance

    // check bundle balance

    // Get all region details
    // getAllRegionDetails(email = "vihang@redotter.sg")
}