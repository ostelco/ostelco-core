package org.ostelco.simcards.admin

import io.dropwizard.Application
import io.dropwizard.jdbi.DBIFactory
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.ostelco.simcards.es2plus.ES2StatusCodeData
import org.ostelco.simcards.es2plus.SmDpPlusCallbackResource
import org.ostelco.simcards.es2plus.SmDpPlusCallbackService
import java.util.stream.Collectors
import java.util.stream.Stream


/**
 * The SIM manager
 * is an application that inputs inhales SIM batches
 * from SIM profile factories (physical or esim). It then facilitates
 * activation of SIM profiles to MSISDNs.   A typical interaction is
 * "find me a sim profile for this MSISDN for this HLR" , and then
 * "activate that profile".   The activation will typically involve
 * at least talking to a HLR to permit user equipment to use the
 * SIM profile to authenticate, and possibly also an SM-DP+ to
 * activate a SIM profile (via its ICCID and possible an EID).
 * The inventory can then serve as an intermidiary between the
 * rest of the BSS and the OSS in the form of HSS and SM-DP+.
 */
class SimAdministrationApplication : Application<SimAdministrationAppConfiguration>() {

    override fun getName(): String {
        return "SIM inventory application"
    }

    override fun initialize(bootstrap: Bootstrap<SimAdministrationAppConfiguration>) {
        // TODO: application initialization
    }

    lateinit var simInventoryDAO: SimInventoryDAO

    override fun run(configuration: SimAdministrationAppConfiguration,
                     environment: Environment) {


        val factory = DBIFactory()
        val jdbi = factory.build(
                environment,
                configuration.database, "sqlite")
        this.simInventoryDAO = jdbi.onDemand(SimInventoryDAO::class.java)


        // XXX Add these parameters to configuration file.
        val oas = OpenAPI()
        val info = Info()
                .title(name)
                .description("SIM management.")
                .termsOfService("http://example.com/terms")
                .contact(Contact().email("rmz@redotter.com"))

        oas.info(info)
        val oasConfig = SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true)
                .resourcePackages(Stream.of("org.ostelco")
                        .collect(Collectors.toSet<String>()))
        val jerseyEnvironment = environment.jersey()
        jerseyEnvironment.register(OpenApiResource()
                .openApiConfiguration(oasConfig))

        // TODO: Placeholder for thing handling callbacks from a remote SM-DP+
        // TODO: Extend this interface to also receive authentication information.
        jerseyEnvironment.register(SimInventoryResource(simInventoryDAO))
        val smdpPlusCallbackHandler = object : SmDpPlusCallbackService {
            override fun handleDownloadProgressInfo(
                    eid: String?,
                    iccid: String?,
                    notificationPointId: String?,
                    profileType: String?,
                    resultData: ES2StatusCodeData?,
                    timestamp: String?,
                    imei: String?) {

            }
        }

        jerseyEnvironment.register(SmDpPlusCallbackResource(smdpPlusCallbackHandler))
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SimAdministrationApplication().run(*args)
        }
    }
}
