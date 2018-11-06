import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.After
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.Blah
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
    fun getsReturnNotifications() {
        val blah = Blah(fooz="da fooz")
        val entity:Entity<Blah> = Entity.entity(blah, MediaType.APPLICATION_JSON!!)
        RULE.target("/foo")
                .request(MediaType.APPLICATION_JSON)
                .header(" X-Admin-Protocol",  "gsma/rsp/v<x.y.z>")
                .post(entity)
    }
}
