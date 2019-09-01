package org.ostelco.simcards.smdpplus;

import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.fail
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.sim.es2plus.ES2PlusClient

public class SmDpPlusTest {

    companion object {
        @JvmField
        @ClassRule
        val SM_DP_PLUS_RULE = DropwizardAppRule(SmDpPlusApplication::class.java,
                ResourceHelpers.resourceFilePath("config.yml"))
    }

    @Test
    fun testThatCorrectNumberOfProfilesAreLoaded() {
        val app: SmDpPlusApplication = SM_DP_PLUS_RULE.getApplication<SmDpPlusApplication>()
        assertEquals(100, app.noOfEntries())
    }


    @Test
    fun testGettingInfo() {
        val localPort = SM_DP_PLUS_RULE.localPort
        val httpClient = HttpClientBuilder(SM_DP_PLUS_RULE.environment).build("Test client")
        val client = ES2PlusClient(httpClient = httpClient, port = localPort, requesterId = "Dunderhonning", useHttps = false)

        val iccid = "8901000000000000001"
        val profileStatus = client.profileStatus(listOf(iccid))
        val profileStatusList = profileStatus.profileStatusList
        assertNotNull(profileStatusList)
        if (profileStatusList == null) {
            fail("profileStatusList == null")
        } else {
            val first = profileStatusList[0]
            assertEquals(iccid, first.iccid)
        }
    }


    /*
       val client: ES2PlusClient =
                SUPPORT.getApplication<SmDpPlusApplication>().es2plusClient
        val eid = "12345678980123456789012345678901"
        val iccid = "8901000000000000001"
        val downloadResponse = client.downloadOrder(eid = eid, iccid = iccid, profileType = "FooTel_STD")

        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, downloadResponse.header.functionExecutionStatus.status)
        assertEquals(iccid, downloadResponse.iccid)

        val confirmResponse =
                client.confirmOrder(
                        eid = eid,
                        iccid = iccid,
                        releaseFlag = true)

        // This happens to be the matching ID used for everything in the test application, not a good
        // assumption for production code, but this isn't that.
        val matchingId = "0123-ABCD-KGBC-IAMSO-SAD0"
        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, confirmResponse.header.functionExecutionStatus.status)
        assertEquals(eid, confirmResponse.eid)
        assertEquals(matchingId, confirmResponse.matchingId)
     */
}
