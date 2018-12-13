package org.ostelco.simcards.smdpplus

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.client.JerseyClientConfiguration
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.jersey.setup.JerseyEnvironment
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.ostelco.jsonschema.RequestServerReaderWriterInterceptor
import org.ostelco.sim.es2plus.ES2PlusIncomingHeadersFilter
import org.ostelco.sim.es2plus.ES2PlusOutgoingHeadersFilter
import org.ostelco.sim.es2plus.SmDpPlusCallbackResource
import org.ostelco.sim.es2plus.SmDpPlusCallbackService
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.validation.Valid
import javax.validation.constraints.NotNull


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

    override fun getName(): String {
        return "SM-DP+ implementation (partial, only for testing of sim admin service)"
    }

    override fun initialize(bootstrap: Bootstrap<SmDpPlusAppConfiguration>) {
        // TODO: application initialization
    }


    override fun run(configuration: SmDpPlusAppConfiguration,
                     environment: Environment) {

        val jerseyEnvironment = environment.jersey()

        addOpenapiResourceToJerseyEnv(jerseyEnvironment)

        val smdpPlusCallbackHandler = object : SmDpPlusCallbackService {
            override fun handleDownloadProgressInfo(
                    eid: String?,
                    iccid: String,
                    notificationPointId: Int,
                    profileType: String?,
                    resultData: String?,
                    timestamp: String) {
                // TODO: Not implemented.

            }
        }

        jerseyEnvironment.register(SmDpPlusCallbackResource(smdpPlusCallbackHandler))
        jerseyEnvironment.register(ES2PlusIncomingHeadersFilter())
        jerseyEnvironment.register(ES2PlusOutgoingHeadersFilter())
        jerseyEnvironment.register(RequestServerReaderWriterInterceptor())
    }

    private fun addOpenapiResourceToJerseyEnv(jerseyEnvironment: JerseyEnvironment) {
        // XXX Add these parameters to configuration file.
        val oas = OpenAPI()
        val info = Info()
                .title(name)
                .description("SM-DP+ (test fixture only)")
                .termsOfService("http://example.com/terms")
                .contact(Contact().email("rmz@redotter.com"))

        oas.info(info)
        val oasConfig = SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true)
                .resourcePackages(Stream.of("org.ostelco")
                        .collect(Collectors.toSet<String>()))



        jerseyEnvironment.register(OpenApiResource()
                .openApiConfiguration(oasConfig))
    }
    

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SmDpPlusApplication().run(*args)
        }
    }
}


class SmDpPlusAppConfiguration : Configuration() {
    @Valid
    @NotNull
    @JsonProperty("database")
    var database: DataSourceFactory = DataSourceFactory()


    @Valid
    @NotNull
    @JsonProperty
    private val httpClient = JerseyClientConfiguration()

    fun getJerseyClientConfiguration(): JerseyClientConfiguration {
        return httpClient
    }

}