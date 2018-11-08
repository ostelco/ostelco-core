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
                .addResource(Es2PlusResource())
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

    // XXX Remove this test, it should be in the test harness for the JsonSchemaValidator, not for the actual ES2+ protocol
    @Test
    fun testDownloadOrderWithIncorrectEid() {
        val es2ProtocolPayload = Es2PlusDownloadOrder(
                "01234567890123456789012345678901a", // Appended an "a" to force a JSON schema validation error
                iccid = "01234567890123456789",
                profileType = "really!")

        postEs2ProtocolCommand("/gsma/rsp2/es2plus/downloadOrder", es2ProtocolPayload, expectedReturnCode = 400)
    }

    @Test
    fun testDownloadOrder() {
        val es2ProtocolPayload = Es2PlusDownloadOrder(
                "01234567890123456789012345678901",
                iccid = "01234567890123456789",
                profileType = "really!")

        val response =
                postEs2ProtocolCommand("/gsma/rsp2/es2plus/downloadOrder", es2ProtocolPayload, expectedReturnCode = 200)
                        .readEntity(Es2DownloadOrderResponse::class.java)
    }

    @Test
    fun testConfirmOrder() {
        val es2ConfirmOrder = postEs2ProtocolCommand("/gsma/rsp2/es2plus/confirmOrder", Es2ConfirmOrder(
                eid = "01234567890123456789012345678901",
                iccid = "01234567890123456789",
                matchingId = "foo",
                confirmationCode = "bar",
                smdsAddress = "baz",
                releaseFlag = true), 200)
                .readEntity(Es2ConfirmOrderResponse::class.java)
    }

    @Test
    fun testCancelOrder() {
        val es2ConfirmOrder = postEs2ProtocolCommand("/gsma/rsp2/es2plus/cancelOrder",
                Es2CancelOrder(
                        eid = "01234567890123456789012345678901",
                        iccid = "01234567890123456789",
                        matchingId = "foo",
                        finalProfileStatusIndicator = "bar"), 200)
                .readEntity(String::class.java)
                // XXX Fails .readEntity(ES2JsonBaseResponse::class.java)
    }
}
