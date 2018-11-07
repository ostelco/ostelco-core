import io.dropwizard.testing.junit.ResourceTestRule
import junit.framework.Assert.assertEquals
import org.everit.json.schema.Schema
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.Es2PlusDownloadOrder
import org.ostelco.Es2PlusResource
import org.ostelco.RestrictedOperationsRequestFilter
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.stream.Collectors
import javax.ws.rs.WebApplicationException
import javax.ws.rs.client.Entity
import javax.ws.rs.container.DynamicFeature
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.FeatureContext
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider
import javax.ws.rs.ext.ReaderInterceptor
import javax.ws.rs.ext.ReaderInterceptorContext
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.kotlinFunction


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

/*

@Secured
@Priority(Priorities.ENTITY_CODER)
public class JsonSchemaValidator implements ContainerRequestFilter {

    @Context
    var  resourceInfo: ResourceInfo

    @Override
    fun filter( requestContext: ContainerRequestContext){

        val method = resourceInfo.getResourceMethod()

        if (method != null) {
            Secured secured = method.getAnnotation(Secured.class);
          // XXX TBD  Check the input stream for the annotation, the
            // replay it for proper serialization.
        }
    }
}
*/

@Provider
class RequestServerReaderInterceptor : ReaderInterceptor, DynamicFeature {

    lateinit var schema: Schema

    init {
        val inputStream = this.javaClass.getResourceAsStream("/es2schemas/ES2+DownloadOrder-def.json")
        val rawSchema = JSONObject(JSONTokener(inputStream))
        schema = org.everit.json.schema.loader.SchemaLoader.load(rawSchema)
    }

    var currentFunc: KFunction<*>? = null
    override fun configure(resourceInfo: ResourceInfo, context: FeatureContext) {
        val method = resourceInfo.resourceMethod
        val func = method.kotlinFunction

        currentFunc = func!!
        println("invoked by method = ${method.kotlinFunction}")
    }

    @Throws(IOException::class, WebApplicationException::class)
    override fun aroundReadFrom(context: ReaderInterceptorContext): Any {
        val originalStream = context.inputStream
        val stream = BufferedReader(InputStreamReader(originalStream)).lines()
        val body: String = stream.collect(Collectors.joining("\n"))
        context.inputStream = ByteArrayInputStream("$body".toByteArray())

        schema.validate(JSONObject(body))

        return context.proceed()
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

    private fun <T> postEs2ProtocolCommand(es2ProtocolPayload: T): Response? {
        val entity: Entity<T> = Entity.entity(es2ProtocolPayload, MediaType.APPLICATION_JSON)
        val result = RULE.target("/gsma/rsp2/es2plus/downloadOrder")
                .request(MediaType.APPLICATION_JSON)
                .header("User-Agent", "gsma-rsp-lpad")
                .header("X-Admin-Protocol", "gsma/rsp/v<x.y.z>")
                .post(entity)
        assertEquals(201, result.status)
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
}
