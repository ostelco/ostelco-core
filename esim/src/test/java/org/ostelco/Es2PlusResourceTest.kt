import io.dropwizard.testing.junit.ResourceTestRule
import junit.framework.TestCase.assertEquals
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.*
import org.ostelco.jsonValidation.RequestServerReaderWriterInterceptor
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


class ES2PlusResourceTest {

    companion object {

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule
                .builder()
                .addResource(Es2PlusResource(SmDpPlus()))
                .addProvider(RestrictedOperationsRequestFilter())
                .addProvider(RequestServerReaderWriterInterceptor())
                .build()

        @JvmStatic
        @AfterClass
        fun afterClass() {
        }
    }

    private fun <T> postEs2ProtocolCommand(
            path: String,
            es2ProtocolPayload: T,
            expectedReturnCode: Int = 201): Response {
        val entity: Entity<T> = Entity.entity(es2ProtocolPayload, MediaType.APPLICATION_JSON)
        val result: Response = RULE.target(path)
                .request(MediaType.APPLICATION_JSON)
                .header("User-Agent", "gsma-rsp-lpad")
                .header("X-Admin-Protocol", "gsma/rsp/v<x.y.z>")
                .post(entity)
        if (expectedReturnCode != result.status) {
            assertEquals("Expected return value $expectedReturnCode, but got ${result.status}.  Body was \"${result.readEntity(String::class.java)}\"", expectedReturnCode, result.status)
        }
        return result
    }


    @Test
    fun testDownloadOrder() {
        val es2ProtocolPayload = Es2PlusDownloadOrder(
                header = ES2RequestHeader(
                        functionRequesterIdentifier = "foo",
                        functionCallIdentifier = "bar"

                ),
                body = Es2PlusDownloadOrderBody(
                        eid = "01234567890123456789012345678901",
                        iccid = "01234567890123456789",
                        profileType = "really!"))

        postEs2ProtocolCommand("/gsma/rsp2/es2plus/downloadOrder",
                es2ProtocolPayload,
                expectedReturnCode = 200)
    }


    @Test
    fun testConfirmOrder() {
        val es2ConfirmOrder = postEs2ProtocolCommand("/gsma/rsp2/es2plus/confirmOrder",
                Es2ConfirmOrder(
                        header = ES2RequestHeader(
                                functionRequesterIdentifier = "foo",
                                functionCallIdentifier = "bar"),
                        body = Es2ConfirmOrderBody(
                                eid = "01234567890123456789012345678901",
                                iccid = "01234567890123456789",
                                matchingId = "foo",
                                confirmationCode = "bar",
                                smdsAddress = "baz",
                                releaseFlag = true)), 200)
                .readEntity(Es2ConfirmOrderResponse::class.java)
    }

    @Test
    fun testCancelOrder() {
        val es2ConfirmOrder = postEs2ProtocolCommand("/gsma/rsp2/es2plus/cancelOrder",
                Es2CancelOrder(
                        header = ES2RequestHeader(
                                functionRequesterIdentifier = "foo",
                                functionCallIdentifier = "bar"

                        ),

                        eid = "01234567890123456789012345678901",
                        iccid = "01234567890123456789",
                        matchingId = "foo",
                        finalProfileStatusIndicator = "bar")
                , 200)
                .readEntity(String::class.java)
    }


    @Test
    fun testReleaseProfile() {
        val es2ConfirmOrder = postEs2ProtocolCommand("/gsma/rsp2/es2plus/releaseProfile",
                Es2ReleaseProfile(
                        header = ES2RequestHeader(
                                functionRequesterIdentifier = "foo",
                                functionCallIdentifier = "bar"
                        ),

                        iccid = "01234567890123456789")
                , 200)
                .readEntity(String::class.java)
    }


    @Test
    fun testHandleDownloadProgressInfo() {
        val es2ConfirmOrder = postEs2ProtocolCommand("/gsma/rsp2/es2plus/handleDownloadProgressInfo",
                Es2HandleDownloadProgressInfo(
                        header = ES2RequestHeader(
                                functionRequesterIdentifier = "foo",
                                functionCallIdentifier = "bar"

                        ))
                , 200)
                .readEntity(String::class.java)
        // XXX Fails .readEntity(ES2JsonBaseResponse::class.java)
    }
}
