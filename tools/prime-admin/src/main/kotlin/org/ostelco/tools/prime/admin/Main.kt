package org.ostelco.tools.prime.admin

import org.ostelco.prime.PrimeApplication
import org.ostelco.tools.prime.admin.actions.addCustomerToSegment
import org.ostelco.tools.prime.admin.actions.approveRegionForCustomer
import org.ostelco.tools.prime.admin.actions.createCustomer
import org.ostelco.tools.prime.admin.actions.createSubscription
import org.ostelco.tools.prime.admin.actions.getAllRegionDetails
import org.ostelco.tools.prime.admin.actions.print
import org.ostelco.tools.prime.admin.actions.printLeft
import org.ostelco.tools.prime.admin.actions.setBalance
import org.ostelco.tools.prime.admin.modules.DwEnvModule
import kotlin.math.pow

/**
 * Update `config/config.yaml` to point to valid Neo4j store and Postgres.
 * For GCP K8s setup, this means setting up port-forwarding which is documented in `/docs/NEO4J.md`.
 * Also, `gcloud auth application-default login` for postgres.
 */
fun main() {
    PrimeApplication().run("server", "config/config.yaml")
    try {
        setupCustomer()
        println("Done")
    } finally {
        DwEnvModule.env.applicationContext.server.stop()
        println("Shutting down")
        System.exit(0)
    }
}

fun setupCustomer() {

    val email = ""
    val nickname = ""
    val regionCode = ""
    val segmentId = ""
    val iccId = ""
    val alias = ""
    val msisdn = ""

    createCustomer(email = email, nickname = nickname).printLeft()
//    deleteCustomer(email = "").printLeft()

    // set bundle balance
    setBalance(email = email, balance = 10 * 2.0.pow(30.0).toLong()).printLeft()

    // check balance

    // link to region
    approveRegionForCustomer(email = email, regionCode = regionCode).printLeft()

    // link to segment
    addCustomerToSegment(email = email, segmentId = segmentId).printLeft()

    // add SimProfile
    createSubscription(
            email = email,
            regionCode = regionCode,
            alias = alias,
            msisdn = msisdn,
            iccId = iccId).printLeft()

    // remove SimProfile

    // Get region details
//    getRegionDetails(email = email, regionCode = regionCode).print()

    // Get all region details
    getAllRegionDetails(email = email).print()
}

fun batchProvision() {

    val email = ""
    val nickname = ""

    createCustomer(email = email, nickname = nickname).printLeft()

    // set bundle balance
    setBalance(email = email, balance = 10 * 2.0.pow(30.0).toLong()).printLeft()

    // check balance

    val data = mapOf(
            "" to listOf(
                    SimProfileData(iccId = "", msisdn = "")
            )
    )

    for (regionCode in data.keys) {

        // link to region
        approveRegionForCustomer(email = email, regionCode = regionCode).printLeft()

        for (index in 0..9) {

            val (iccId, msisdn) = data[regionCode]?.get(index) ?: throw Exception()

            // add SimProfile
            createSubscription(
                    email = email,
                    regionCode = regionCode,
                    alias = "SIM ${index + 1} for $regionCode",
                    msisdn = msisdn,
                    iccId = iccId).printLeft()

        }
    }

    // Get all region details
    getAllRegionDetails(email = email).print()
}

fun doActions() {

//    check()
//    sync()
//    setup()
//    index()

}

data class SimProfileData(val iccId: String, val msisdn: String)