import io.dropwizard.testing.junit.ResourceTestRule
import junit.framework.TestCase.assertEquals
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.*
import org.ostelco.jsonValidation.JsonSchemaInputOutputValidationInterceptor
import javax.ws.rs.client.Entity
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


class ES2PlusResourceTest {

    companion object {
        @JvmField
        @ClassRule
        val RULE: ResourceTestRule = ResourceTestRule
                .builder()
                .addResource(EsimInventoryResource())
                .addProvider(JsonSchemaInputOutputValidationInterceptor("resources"))
                .build()

        @JvmStatic
        @AfterClass
        fun afterClass() {
        }
    }

    @Test
    fun testFindByIccid() {
        val response = RULE.target("/ostelco/sim-inventory/find-by-iccid")
                .request(MediaType.APPLICATION_JSON)
                // XXX Add parameter for iccid
                .get()

        assertEquals(200, response.status)

        val simEntry = response.readEntity(SimEntry::class.java)
        println("entry = ${simEntry}")
    }
}
