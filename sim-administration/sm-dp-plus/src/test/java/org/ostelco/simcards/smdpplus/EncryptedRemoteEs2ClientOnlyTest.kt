package org.ostelco.simcards.smdpplus

import io.dropwizard.testing.DropwizardTestSupport
import junit.framework.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.ostelco.sim.es2plus.ES2PlusClient
import org.ostelco.sim.es2plus.FunctionExecutionStatusType

// XXX This test should be placed in the es2plus4dropwizard library, and
//     should be run as part of testing that application, not the SM-DP-Plus application.

class EncryptedRemoteEs2ClientOnlyTest {

    val iccid = "8947000000000000038"


    private lateinit var client: ES2PlusClient

    @Before
    fun setUp() {
        SUPPORT.before()
        this.client = SUPPORT.getApplication<SmDpPlusApplication>().es2plusClient
    }

    @After
    fun tearDown() {
        SUPPORT.after()
    }


    fun getState(): String {
        val profileStatus =
                client.profileStatus(iccidList = listOf(iccid))
        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, profileStatus.header.functionExecutionStatus.status)
        assertEquals(1, profileStatus.profileStatusList!!.size)

        var profileStatusResponse = profileStatus.profileStatusList!!.get(0)

        assertTrue(profileStatusResponse.iccid!!.startsWith(iccid))
        assertNotNull(profileStatusResponse.state)
        return profileStatusResponse.state!!
    }
    

    /**
     * Run the typical scenario we run when allocating a sim profile.
     * The only exception is the optional move to "available" if not already
     * in that state.
     */
    @Test
    fun handleHappyDayScenario() {

        if ("AVAILABLE" != getState()) {
            setStateToAvailable()
        }
        downloadProfile()
        confirmOrder()
    }

    private fun confirmOrder() {
        val confirmResponse =
                client.confirmOrder(
                        iccid = iccid,
                        releaseFlag = true)

        // This happens to be the matching ID used for everything in the test application, not a good
        // assumption for production code, but this isn't that.
        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, confirmResponse.header.functionExecutionStatus.status)

        assertEquals("RELEASED", getState())
    }

    private fun setStateToAvailable() {
        val cancelOrderResult =
                client.cancelOrder(
                        iccid = iccid,
                        finalProfileStatusIndicator = "AVAILABLE"
                )
        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, cancelOrderResult.header.functionExecutionStatus.status)


        assertEquals("AVAILABLE", getState())
    }

    private fun downloadProfile() {
        val downloadResponse = client.downloadOrder(iccid = iccid)

        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, downloadResponse.header.functionExecutionStatus.status)
        assertTrue(downloadResponse.iccid!!.startsWith(iccid))

        assertEquals("ALLOCATED", getState())
    }

    companion object {
        val SUPPORT = DropwizardTestSupport<SmDpPlusAppConfiguration>(
                SmDpPlusApplication::class.java,
                "config-external-smdp.yml"
        )
    }
}