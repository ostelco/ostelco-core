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
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.stream.Collectors
import javax.ws.rs.WebApplicationException
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider
import javax.ws.rs.ext.ReaderInterceptor
import javax.ws.rs.ext.ReaderInterceptorContext


/**
 * Testing that we're able to stay within the envelope as defined by the
 * standardf rom GSMA:
 *
 * HTTP POST <HTTP Path> HTTP/1.1
 * Host: <Server Address>
 * User-Agent: gsma-rsp-lpad
 * X-Admin-Protocol: gsma/rsp/v<x.y.z>
 * Content-Type: application/json
 * Content-Length: <Length of the JSON requestMessage>
 * <JSON requestMessage>
 */

g
@Provider
class RequestServerReaderInterceptor : ReaderInterceptor {

    private val schemaRoot = "/es2schemas"
    private var schemaMap: MutableMap<String, Schema> = mutableMapOf()


    private fun loadJsonSchemaResource(name: String) : Schema {
        val inputStream = this.javaClass.getResourceAsStream("${schemaRoot}/${name}.json")
        if (inputStream == null) {
            throw WebApplicationException("Unknown schema map", Response.Status.INTERNAL_SERVER_ERROR)
        }
        try {
            val schema = JSONObject(JSONTokener(inputStream))
            return org.everit.json.schema.loader.SchemaLoader.load(schema)
        } catch (e: JSONException) {
            throw WebApplicationException("Syntax error in schema description", Response.Status.INTERNAL_SERVER_ERROR)
        }
    }


    private fun getSchema(name: String): Schema {
        if (!schemaMap.containsKey(name)) {
             schemaMap[name] = loadJsonSchemaResource(name)
        }
        return schemaMap[name]!!
    }


    @Throws(IOException::class, WebApplicationException::class)
    override fun aroundReadFrom(ctx: ReaderInterceptorContext): Any {
        val originalStream = ctx.inputStream
        val stream = BufferedReader(InputStreamReader(originalStream)).lines()
        val body: String = stream.collect(Collectors.joining("\n"))
        ctx.inputStream = ByteArrayInputStream("$body".toByteArray())

        // If we have a validation schema, then use it!
        val schemaAnnotation = ctx.type.getAnnotation<JsonSchema>(JsonSchema::class.java)
        if (schemaAnnotation != null) {
            try {
                getSchema(schemaAnnotation.schemaKey).validate(JSONObject(body))
            } catch (t: ValidationException) {
                throw WebApplicationException(t.errorMessage, Response.Status.BAD_REQUEST)
            }
        }

        return ctx.proceed()
    }

}

class ES2PlusResourceTest {

    companion object {

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule
                .builder()
                .addResource(Es2PlusResource())
                .addProvider(RestrictedOperationsRequestFilter())
                .addProvider(RequestServerReaderInterceptor())
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
