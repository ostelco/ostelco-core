import ES2PlusResourceTest.Companion.RULE
import io.dropwizard.testing.junit.ResourceTestRule
import junit.framework.Assert.assertEquals
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.Es2PlusDownloadOrder
import org.ostelco.Es2PlusResource
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import java.io.IOException
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider


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



@Provider
class RestrictedOperationsRequestFilter : ContainerRequestFilter {

    @Throws(IOException::class)
    override fun filter(ctx: ContainerRequestContext) {
        val adminProtocol = ctx.headers.getFirst("X-Admin-Protocol")
        val userAgent = ctx.headers.getFirst("User-Agent")

        if (!"gsma-rsp-lpad".equals(userAgent)) {
            ctx.abortWith(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Illegal user agent, expected gsma-rsp-lpad")
                    .build())
        } else if (adminProtocol == null || !adminProtocol!!.startsWith("gsma/rsp/")) {
                    ctx.abortWith(Response.status(Response.Status.BAD_REQUEST)
                            .entity("Illegal X-Admin-Protocol header, expected something starting with \"gsma/rsp/\"")
                            .build())
                }
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
                .build()

        @JvmStatic
        @AfterClass
        fun afterClass() {}
    }



    private  fun <T> postEs2ProtocolCommand(es2ProtocolPayload: T): Response? {
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
        val es2ProtocolPayload = Es2PlusDownloadOrder("secret eid", iccid = "highly conformant iccid", profileType = "really!")
        postEs2ProtocolCommand(es2ProtocolPayload)
    }
}
