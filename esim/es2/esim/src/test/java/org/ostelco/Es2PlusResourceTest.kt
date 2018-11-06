import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.After
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.Es2PlusResource
import javax.ws.rs.core.MediaType


class ES2PlusResourceTest {

    @Test
    fun getsReturnNotifications() {
        RULE.target("/foo")
                .request(MediaType.APPLICATION_JSON)
                .get(String::class.java)
    }

    @After
    fun after() {}

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
}
