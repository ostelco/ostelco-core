package org.ostelco.tools.prime.admin

import org.ostelco.prime.PrimeApplication

/**
 * Update `config/config.yaml` to point to valid Neo4j store and Postgres.
 * For GCP K8s setup, this means setting up port-forwarding which is documented in `/docs/NEO4J.md`.
 * Also, `gcloud auth application-default login` for postgres.
 */
fun main() {

//    val bytes = Base64
//            .getDecoder()
//            .decode("")
//
//    Files.write(Path.of("src/main/resources/dev-idemia-client-crt.jks"), bytes)

    PrimeApplication().run("server", "config/config.yaml")
    try {
        doActions()
    } finally {
        // DwEnvModule.env.applicationContext.server.stop()
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

    // add SimProfile
//    createSubscription(
//            email = "",
//            regionCode ="",
//            alias = "",
//            msisdn = "").printLeft()

    // remove SimProfile

    // set bundle balance

    // check bundle balance

    // Get region details
//    getRegionDetails(email = "", regionCode = "").print()

    // Get all region details
//     getAllRegionDetails(email = "").print()

}