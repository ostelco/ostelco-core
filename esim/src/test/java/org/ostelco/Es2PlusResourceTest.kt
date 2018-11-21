
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

    val client = ES2PlusClient("Integration test client", RULE.client())


    fun <T> postEs2ProtocolCommand(
            path: String,
            es2ProtocolPayload: T,
            expectedReturnCode: Int = 201): Response {
        val entity: Entity<T> = Entity.entity(es2ProtocolPayload, MediaType.APPLICATION_JSON)
        val result: Response = RULE.client().target(path)
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
        val result = client.downloadOrder(
                eid = "01234567890123456789012345678901",
                iccid = "01234567890123456789",
                profileType = "AProfileTypeOfSomeSort")
        println("result = $result")
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
    }

    @Test
    fun testCancelOrder() {
        client.cancelOrder(
                eid = "01234567890123456789012345678901",
                iccid = "01234567890123456789",
                matchingId = "foo",
                finalProfileStatusIndicator = "bar")
    }

    @Test
    fun testReleaseProfile() {
        client.releaseProfile(iccid = "01234567890123456789")
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
