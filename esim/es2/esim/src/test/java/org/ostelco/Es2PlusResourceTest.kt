import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.Es2PlusDownloadOrder
import org.ostelco.Es2PlusResource
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType


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


class ES2PlusResourceTest {



    companion object {

        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule
                .builder()
                .addResource(Es2PlusResource())
                .build()

        @JvmStatic
        @AfterClass
        fun afterClass() {}
    }

    @Test
    fun testDownloadOrder() {
        val es2ProtocolPayload = Es2PlusDownloadOrder("secret eid", iccid = "highly conformant iccid", profileType = "really!")
        postEs2ProtocolCommand(es2ProtocolPayload)
    }

    private  fun <T> postEs2ProtocolCommand(es2ProtocolPayload: T) {
        val entity: Entity<T> = Entity.entity(es2ProtocolPayload, MediaType.APPLICATION_JSON)
        RULE.target("/gsma/rsp2/es2plus/downloadOrder")
                .request(MediaType.APPLICATION_JSON)
                .header("User-Agent:", "gsma-rsp-lpad")
                .header("X-Admin-Protocol", "gsma/rsp/v<x.y.z>")
                .post(entity)
    }
}
