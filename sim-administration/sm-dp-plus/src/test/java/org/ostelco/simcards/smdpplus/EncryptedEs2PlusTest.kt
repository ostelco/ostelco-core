package org.ostelco.simcards.smdpplus

import io.dropwizard.testing.DropwizardTestSupport
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.ostelco.sim.es2plus.ES2PlusClient
import org.ostelco.sim.es2plus.FunctionExecutionStatusType

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

        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, downloadResponse.header.functionExecutionStatus.status)
        assertEquals(iccid, downloadResponse.iccid)

        val matchingId = "ABCD-EFGH-01234-5679-0987-6543"
        val confirmResponse =
                client.confirmOrder(
                        iccid = iccid,
                        releaseFlag = true)

        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, confirmResponse.header.functionExecutionStatus.status)
        assertEquals(eid, confirmResponse.eid)
        assertEquals(matchingId, confirmResponse.matchingId)
    }

    companion object {
        val SUPPORT = DropwizardTestSupport<SmDpPlusAppConfiguration>(
                SmDpPlusApplication::class.java,
                "config.yml"
        )
    }
}