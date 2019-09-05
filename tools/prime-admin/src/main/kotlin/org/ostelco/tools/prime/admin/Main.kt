package org.ostelco.tools.prime.admin

import org.ostelco.prime.PrimeApplication
import org.ostelco.tools.prime.admin.modules.DwEnvModule

/**
 * Update `config/config.yaml` to point to valid Neo4j store and Postgres.
 * For GCP K8s setup, this means setting up port-forwarding which is documented in `/docs/NEO4J.md`.
 * Also, `gcloud auth application-default login` for postgres.
 */
fun main() {
    PrimeApplication().run("server", "config/config.yaml")
    try {
        doActions()
        println("Done")
    } finally {
        DwEnvModule.env.applicationContext.server.stop()
        println("Shutting down")
        System.exit(0)
    }
}

fun doActions() {

//    check()
//    sync()
//    setup()
//    index()

//    createCustomer(email = "", nickname = "").printLeft()
//    deleteCustomer(email = "").printLeft()

    // link to region
//     approveRegionForCustomer(email = "", regionCode = "").printLeft()

    // add SimProfile
//    createSubscription(
//            email = "",
//            regionCode = "",
//            alias = "",
//            msisdn = "").printLeft()

    // remove SimProfile

    // set bundle balance
//    setBalance(email = "", balance = 10 * 2.0.pow(30.0).toLong()).printLeft()

    // check bundle balance

    // Get region details
//    getRegionDetails(email = "", regionCode = "").print()

    // Get all region details
//     getAllRegionDetails(email = "").print()

}