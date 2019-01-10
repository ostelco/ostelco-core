import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.HttpClientConfiguration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.apache.http.client.HttpClient
import org.conscrypt.OpenSSLProvider
import org.ostelco.dropwizardutils.*
import org.ostelco.dropwizardutils.OpenapiResourceAdder.Companion.addOpenapiResourceToJerseyEnv
import org.ostelco.sim.es2plus.ES2PlusIncomingHeadersFilter.Companion.addEs2PlusDefaultFiltersAndInterceptors
import org.ostelco.sim.es2plus.SmDpPlusServerResource
import org.ostelco.sim.es2plus.SmDpPlusService
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.security.Security
import javax.annotation.security.RolesAllowed
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.SecurityContext


/**
 * NOTE: This is not a proper SM-DP+ application, it is a test fixture
 * to be used when accpetance-testing the sim administration application.
 *
 * The intent of the SmDpPlusApplication is to be run in Docker Compose,
 * to serve a few simple ES2+ commands, and to do so consistently, and to
 * report back to the sim administration application via ES2+ callback, as to
 * exercise that part of the protocol as well.
 *
 * In no shape or form is this intended to be a proper SmDpPlus application. It
 * does not store sim profiles, it does not talk ES9+ or ES8+ or indeed do
 * any of the things that would be useful for serving actual eSIM profiles.
 *
 * With those caveats in mind, let's go on to the important task of making a simplified
 * SM-DP+ that can serve as a test fixture :-)
 */
class SmDpPlusApplication : Application<SmDpPlusAppConfiguration>() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getName(): String {
        return "SM-DP+ implementation (partial, only for testing of sim admin service)"
    }

    override fun initialize(bootstrap: Bootstrap<SmDpPlusAppConfiguration>) {
        // TODO: application initialization
    }

    lateinit var client: HttpClient

    override fun run(config: SmDpPlusAppConfiguration,
                     env: Environment) {

        val jerseyEnvironment = env.jersey()

        // XXX Only until we're sure the client stuff works.
        jerseyEnvironment.register(PingResource())
        jerseyEnvironment.register(
                CertificateAuthorizationFilter(
                    RBACService(
                            rolesConfig = config.rolesConfig,
                            certConfig = config.certConfig)))

        this.client = HttpClientBuilder(env).using(
                config.httpClientConfiguration).build(getName())
    }


    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {

            Security.insertProviderAt(OpenSSLProvider(), 1)

            SmDpPlusApplication().run(*args)
        }
    }
}


// XXX Can be removed once we're sure the client works well with
//     encryption, and a test for that has been extended to work
//     also for something other than ping.
@Path("/ping")
class PingResource {

    private val log = LoggerFactory.getLogger(javaClass)

    @RolesAllowed("flyfisher")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun ping(//@Auth user: Authentication.User,
             @Context context:SecurityContext ): String  {
        return  "pong"
    }
}

/**
 * Configuration class for SM-DP+ emulator.
 */
class SmDpPlusAppConfiguration : Configuration() {

    /**
     * The client we use to connect to other services, including
     * ES2+ services
     */
    @Valid
    @NotNull
    @JsonProperty("httpClient")
    var httpClientConfiguration = HttpClientConfiguration()


    /**
     * Declaring the mapping between users and certificates, also
     * which roles the users are assigned to.
     */
    @Valid
    @JsonProperty("certAuth")
    @NotNull
    var certConfig = CertAuthConfig()

    /**
     * Declaring which roles we will permit
     */
    @Valid
    @JsonProperty("roles")
    @NotNull
    var rolesConfig = RolesConfig()

}