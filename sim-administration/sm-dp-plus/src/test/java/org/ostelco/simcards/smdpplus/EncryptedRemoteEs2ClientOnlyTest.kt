package org.ostelco.simcards.smdpplus

import io.dropwizard.testing.DropwizardTestSupport
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.ostelco.sim.es2plus.FunctionExecutionStatusType

// XXX This test should be placed in the es2plus4dropwizard library, and
//     should be run as part of testing that application, not the SM-DP-Plus application.

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
        val iccid = "8947000000000000020"

        val profileStatus =
                client.profileStatus(iccidList = listOf(iccid))
        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, profileStatus.header.functionExecutionStatus.status)
        assertEquals(1, profileStatus.profileStatusList!!.size)
        assertEquals(iccid, profileStatus.profileStatusList!!.get(0).iccid)

        // XXX TODO: fill in the blanks for a happy day scenario.
        // If profile not available, then reset it to state available, then run through a
        // happy day provisioning scenario.


        val downloadResponse = client.downloadOrder(iccid = iccid)

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