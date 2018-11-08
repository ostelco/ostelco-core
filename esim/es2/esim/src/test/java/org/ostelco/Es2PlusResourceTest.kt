import io.dropwizard.testing.junit.ResourceTestRule
import junit.framework.TestCase.assertEquals
import org.everit.json.schema.Schema
import org.everit.json.schema.ValidationException
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.Es2PlusDownloadOrder
import org.ostelco.Es2PlusResource
import org.ostelco.JsonSchema
import org.ostelco.RestrictedOperationsRequestFilter
import org.ostelco.jsonValidation.RequestServerReaderWriterInterceptor
import java.io.*
import java.util.stream.Collectors
import javax.ws.rs.WebApplicationException
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.*


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
            es2ProtocolPayload: T,
            expectedReturnCode: Int = 201): Response? {
        val entity: Entity<T> = Entity.entity(es2ProtocolPayload, MediaType.APPLICATION_JSON)
        val result = RULE.target("/gsma/rsp2/es2plus/downloadOrder")
                .request(MediaType.APPLICATION_JSON)
                .header("User-Agent", "gsma-rsp-lpad")
                .header("X-Admin-Protocol", "gsma/rsp/v<x.y.z>")
                .post(entity)
        assertEquals(expectedReturnCode, result.status)
        return result
    }

    // XXX TODO:   Extend the method to return a proper result object,
    //             pick that result object up, and fail the test if it's
    //             the expected result.
    @Test
    fun testDownloadOrder() {
        val es2ProtocolPayload = Es2PlusDownloadOrder(
                "01234567890123456789012345678901",
                iccid = "01234567890123456789",
                profileType = "really!")

        postEs2ProtocolCommand(es2ProtocolPayload)
    }

    @Test
    fun testDownloadOrderWithIncorrectEid() {
        val es2ProtocolPayload = Es2PlusDownloadOrder(
                "01234567890123456789012345678901a", // Appended an "a" to force a JSON schema validation error
                iccid = "01234567890123456789",
                profileType = "really!")

        postEs2ProtocolCommand(es2ProtocolPayload, expectedReturnCode=400)
    }
}
