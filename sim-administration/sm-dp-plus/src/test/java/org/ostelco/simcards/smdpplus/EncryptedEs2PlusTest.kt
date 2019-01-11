package org.ostelco.simcards.smdpplus

import io.dropwizard.testing.DropwizardTestSupport
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.ostelco.sim.es2plus.ES2PlusClient

class EncryptedEs2PlusTest {

    @Before
    fun setUp() {
        SUPPORT.before()
    }

    @After
    fun tearDown() {
        SUPPORT.after()
    }

    /**
     * These are the two scenarios that needs to be run in sequence from a
     * sim-adminstrator, in pretty much the same way as it being run here.
     */
    @Test
    fun handleHappyDayScenario() {
        val client: ES2PlusClient =
                SUPPORT.getApplication<SmDpPlusApplication>().es2plusClient
        val eid = "12345678980123456789012345678901"
        val iccid = "8901000000000000001"
        val downloadResponse = client.downloadOrder(eid = eid, iccid = iccid, profileType = "FooTel_STD")

        // XXX Check for correct return values

        val matchingCode = "ABCD-EFGH-01234-5679-0987-6543"
        val confirmResponse =
                client.confirmOrder(
                        eid = eid,  // XXX Should be null -> Not part of the invocation
                        iccid = iccid,
                        confirmationCode = "4711", // should be null
                        matchingId = matchingCode,  // XXX Should be null -> Not part of the invocation
                        smdsAddress = "localhost", // XXX Should be null -> Not part of the invocation
                        releaseFlag = true)

        // XXX Check return values
    }

    companion object {
        val SUPPORT = DropwizardTestSupport<SmDpPlusAppConfiguration>(
                SmDpPlusApplication::class.java,
                "config.yml"
        )
    }
}