package org.ostelco.sim.es2plus

import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.AfterClass
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.reset
import org.ostelco.jsonschema.DynamicES2ValidatorAdder


class ES2PlusResourceTest {


    private val iccid = "01234567890123456789"
    private val eid = "01234567890123456789012345678901"
    private val matchingId = "ABCD-EFGH-IJKL-MNOP-1234"
    private val confirmationCode = "bar"


    companion object {

        val smdpPlusService: SmDpPlusService = Mockito.mock(SmDpPlusService::class.java)
        val callbackService: SmDpPlusCallbackService = Mockito.mock(SmDpPlusCallbackService::class.java)

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule
                .builder()
                .addResource(SmDpPlusServerResource(smdpPlusService))
                .addResource(SmDpPlusCallbackResource(callbackService))
                .addProvider(ES2PlusIncomingHeadersFilter())
                .addProvider(DynamicES2ValidatorAdder())
                .addProvider(ES2PlusOutgoingHeadersFilter())
                .build()

        @JvmStatic
        @AfterClass
        fun afterClass() {
        }
    }

    @Before
    fun setUp() {
        reset(smdpPlusService)
        reset(callbackService)
    }

    private val client = ES2PlusClient(
            requesterId = "Integration test client",
            jerseyClient = RULE.client())

    @Test
    fun testDownloadOrder() {

        Mockito.`when`(smdpPlusService.downloadOrder(
                eid = Mockito.anyString(),
                iccid = Mockito.anyString(),
                profileType = Mockito.anyString()))
                .thenReturn(Es2DownloadOrderResponse(
                        header = eS2SuccessResponseHeader(),
                        iccid = iccid
                ))

        val result = client.downloadOrder(
                eid = eid,
                iccid = iccid,
                profileType = "AProfileTypeOfSomeSort")
    }


    @Test
    fun testConfirmOrder() {

        Mockito.`when`(smdpPlusService.confirmOrder(
                eid = Mockito.anyString(),
                iccid = Mockito.anyString(),
                smdsAddress = Mockito.anyString(),
                machingId = Mockito.anyString(),
                confirmationCode = Mockito.anyString(),
                releaseFlag = Mockito.anyBoolean()))
                .thenReturn(
                        Es2ConfirmOrderResponse(
                                header = eS2SuccessResponseHeader(),
                                eid = "12345678901234567890123456789012",
                                matchingId = "BANANAS-ARE-GREAT"))


        client.confirmOrder(
                eid = eid,
                iccid = iccid,
                matchingId = matchingId,
                confirmationCode = confirmationCode,
                smdpAddress = "baz",
                releaseFlag = true)
    }

    @Test
    fun testCancelOrder() {
        client.cancelOrder(
                eid = eid,
                iccid = iccid,
                matchingId = matchingId,
                finalProfileStatusIndicator = confirmationCode)
        // XXX Do some verification
    }

    @Test
    fun testReleaseProfile() {
        client.releaseProfile(iccid = iccid)
        // XXX Do some verification
    }

    @Test
    fun testHandleDownloadProgressInfo() {
        // XXX Not testing anything sensible
        client.handleDownloadProgressInfo(
                iccid = iccid,
                eid = eid,
                profileType = "profileType",
                timestamp = "2001-12-17T09:30:47Z",
                notificationPointId = 4711,
                notificationPointStatus = ES2NotificationPointStatus()
        )
        // XXX Do some verification
    }


    @Test
    fun testHandleDownloadProgressInfoForIccidWithSuffixF() {
        // XXX Not testing anything sensible
        client.handleDownloadProgressInfo(
                iccid = iccid + "F",
                eid = eid,
                profileType = "profileType",
                timestamp = "2001-12-17T09:30:47Z",
                notificationPointId = 4711,
                notificationPointStatus = ES2NotificationPointStatus()
        )
        // XXX Do some verification
    }


    @Test(expected = ES2PlusClientException::class)
    fun testHandleDownloadProgressInfoForIccidWithSuffixZ() {
        // XXX Not testing anything sensible
        client.handleDownloadProgressInfo(
                iccid = iccid + "Z",
                eid = eid,
                profileType = "profileType",
                timestamp = "2001-12-17T09:30:47Z",
                notificationPointId = 4711,
                notificationPointStatus = ES2NotificationPointStatus()
        )
        // XXX Do some verification
    }

    // XXX Not testing error cases, to ensure that the exception, error reporting
    //     mechanism is working properly.
}
