package org.ostelco.tools.prime.admin

import org.ostelco.prime.PrimeApplication
import org.ostelco.tools.prime.admin.actions.getAllRegionDetails
import org.ostelco.tools.prime.admin.modules.DwEnvModule
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Update `config/config.yaml` to point to valid Neo4j store and Postgres.
 * For GCP K8s setup, this means setting up port-forwarding which is documented in `/docs/NEO4J.md`.
 * Also, `gcloud auth application-default login` for postgres.
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
//    createSubscription(
//            email = "",
//            regionCode ="",
//            alias = "",
//            msisdn = "")

    // remove SimProfile

    // set bundle balance

    // check bundle balance

    // Get region details
//    getRegionDetails(email = "", regionCode = "")

    // Get all region details
//     getAllRegionDetails(email = "")

}