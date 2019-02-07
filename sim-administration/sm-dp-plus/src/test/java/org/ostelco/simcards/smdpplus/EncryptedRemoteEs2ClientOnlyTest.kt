package org.ostelco.simcards.smdpplus

import io.dropwizard.testing.DropwizardTestSupport
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.ostelco.sim.es2plus.FunctionExecutionStatusType

class EncryptedRemoteEs2ClientOnlyTest {

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

        val client = SUPPORT.getApplication<SmDpPlusApplication>().es2plusClient

        val iccid = "8901000000000000001"
        val downloadResponse = client.downloadOrder(iccid = iccid, profileType = "Loltel_STD")

        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, downloadResponse.header.functionExecutionStatus.status)
        assertEquals(iccid, downloadResponse.iccid)


        val confirmResponse =
                client.confirmOrder(
                        iccid = iccid,
                        releaseFlag = true)

        // This happens to be the matching ID used for everything in the test application, not a good
        // assumption for production code, but this isn't that.
        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, confirmResponse.header.functionExecutionStatus.status)
    }

    companion object {
        val SUPPORT = DropwizardTestSupport<SmDpPlusAppConfiguration>(
                SmDpPlusApplication::class.java,
                "config-external-smdp.yml"
        )
    }
}