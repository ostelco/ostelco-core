package org.ostelco.simcards.es2plus

import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.AfterClass
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.reset
import org.ostelco.simcards.jsonValidation.RequestServerReaderWriterInterceptor


class ES2PlusResourceTest {

    companion object {

        val smdpPlusService = Mockito.mock(SmDpPlusService::class.java)
        val callbackService = Mockito.mock(SmDpPlusCallbackService::class.java)

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule
                .builder()
                .addResource(SmDpPlusServerResource(smdpPlusService))
                .addResource(SmDpPlusCallbackResource(callbackService))
                .addProvider(RestrictedOperationsRequestFilter())
                .addProvider(RequestServerReaderWriterInterceptor())
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

    val client = ES2PlusClient("Integration test client", RULE.client())


    @Test
    fun testDownloadOrder() {

        Mockito.`when`(smdpPlusService.downloadOrder(
                eid = Mockito.anyString(),
                iccid = Mockito.anyString(),
                profileType = Mockito.anyString()))
                .thenReturn("01234567890123456789")

        val result = client.downloadOrder(
                eid = "01234567890123456789012345678901",
                iccid = "01234567890123456789",
                profileType = "AProfileTypeOfSomeSort")
        // XXX Do some verification
    }

    @Test
    fun testConfirmOrder() {

        client.confirmOrder(
                eid = "01234567890123456789012345678901",
                iccid = "01234567890123456789",
                matchingId = "foo",
                confirmationCode = "bar",
                smdsAddress = "baz",
                releaseFlag = true)
        // XXX Do some verification
    }

    @Test
    fun testCancelOrder() {
        client.cancelOrder(
                eid = "01234567890123456789012345678901",
                iccid = "01234567890123456789",
                matchingId = "foo",
                finalProfileStatusIndicator = "bar")
        // XXX Do some verification
    }

    @Test
    fun testReleaseProfile() {
        client.releaseProfile(iccid = "01234567890123456789")
        // XXX Do some verification
    }

    @Test
    fun testHandleDownloadProgressInfo() {
        // XXX Not testing anything sensible
        client.handleDownloadProgressInfo()
        // XXX Do some verification
    }


    // XXX Not testing error cases, to ensure that the exception, error reportibng
    //     mechanism is working properly.
}
