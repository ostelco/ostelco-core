import io.dropwizard.testing.ResourceHelpers.resourceFilePath
import io.dropwizard.testing.junit.DropwizardAppRule
import org.assertj.core.api.Assertions.assertThat
import org.glassfish.jersey.client.ClientResponse
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.Blah
import org.ostelco.Es2plusApplication
import org.ostelco.Es2plusConfiguration
import javax.ws.rs.client.Client
import org.glassfish.jersey.client.JerseyClientBuilder
import io.dropwizard.testing.junit.ResourceTestRule





class LoginAcceptanceTest {

    public companion object {
        @JvmField
        @ClassRule
        val RULE = DropwizardAppRule<Es2plusConfiguration>(
                Es2plusApplication::class.java,
                resourceFilePath("config.yaml"))
    }




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

    @Test
    fun loginHandlerRedirectsAfterPost() {
        val client = JerseyClientBuilder().build()

        // XXX Do the serialization/deserialization, during testing
        //     check that ther responses we receive are in fact
        //     legal as defined by the json schema spacificatkon
        //     for the ES2plus protocol.
        val response = client.resource(
                String.format("http://localhost:%d/foo", RULE.getLocalPort()))
                // XXX Host
                //  X-Admin-Protocol: gsma/rsp/v<x.y.z>
                .post(ClientResponse::class.java, Blah("gazonk"))

        assertThat(response.getStatus()).isEqualTo(302)
    }
    */
}