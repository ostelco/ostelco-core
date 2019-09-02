package org.ostelco.simcards.smdpplus;

import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.fail
import org.apache.http.impl.client.CloseableHttpClient
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.sim.es2plus.ES2PlusClient
import org.ostelco.sim.es2plus.FunctionExecutionStatusType

public class SmDpPlusTest {

    companion object {
        @JvmField
        @ClassRule
        val SM_DP_PLUS_RULE = DropwizardAppRule(SmDpPlusApplication::class.java,
                ResourceHelpers.resourceFilePath("config.yml"))
    }

    private val httpClient: CloseableHttpClient
    private val localPort: Int
    private val client: ES2PlusClient

    // First iccid in the .csv file used to prime the sm-dp+.   Used in
    // various tests.
    val firstIccid = "8901000000000000001"

    // This happens to be the matching ID used for everything in the test application, not a good
    // assumption for production code, but this isn't that.  XXX Should be fixed!!
    val magicMatchingID = "0123-ABCD-KGBC-IAMOS-SAD0"

    init {
        this.httpClient = HttpClientBuilder(SM_DP_PLUS_RULE.environment).build("Test client")
        this.localPort = SM_DP_PLUS_RULE.localPort
        this.client = ES2PlusClient(httpClient = httpClient, port = localPort, requesterId = "Dunderhonning", useHttps = false)
    }


    @Test
    fun testThatCorrectNumberOfProfilesAreLoaded() {
        val app: SmDpPlusApplication = SM_DP_PLUS_RULE.getApplication<SmDpPlusApplication>()
        assertEquals(100, app.noOfEntries())
    }


    @Test
    fun testGettingInfo() {
        val profileStatus = client.profileStatus(listOf(firstIccid))
        val profileStatusList = profileStatus.profileStatusList
        assertNotNull(profileStatusList)
        if (profileStatusList == null) {
            fail("profileStatusList == null")
        } else {
            val first = profileStatusList[0]
            assertEquals(firstIccid, first.iccid)
        }
    }


    @Test
    fun testFullRoundtrip() {
        val eid = "12345678980123456789012345678901"
        val downloadResponse = client.downloadOrder(eid = eid, iccid = firstIccid, profileType = "FooTel_STD")

        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, downloadResponse.header.functionExecutionStatus.status)
        assertEquals(firstIccid, downloadResponse.iccid)

        val confirmResponse =
                client.confirmOrder(
                        eid = eid,
                        iccid = firstIccid,
                        releaseFlag = true)

        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, confirmResponse.header.functionExecutionStatus.status)
        assertEquals(eid, confirmResponse.eid)
        assertEquals(magicMatchingID, confirmResponse.matchingId)
    }
}
