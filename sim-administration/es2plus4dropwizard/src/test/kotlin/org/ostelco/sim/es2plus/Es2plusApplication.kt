package org.ostelco.sim.es2plus

import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.ostelco.sim.es2plus.ES2PlusIncomingHeadersFilter.Companion.addEs2PlusDefaultFiltersAndInterceptors
import java.util.stream.Collectors
import java.util.stream.Stream

class Es2plusApplication : Application<Es2plusConfiguration>() {

    override fun getName(): String {
        return "es2+ application"
    }

    override fun initialize(bootstrap: Bootstrap<Es2plusConfiguration>) {
        // TODO: application initialization
    }

    override fun run(configuration: Es2plusConfiguration,
                     environment: Environment) {

        // XXX Add these parameters to configuration file.
        val oas = OpenAPI()
        val info = Info()
                .title(name)
                .description("Restful membership management.")
                .termsOfService("http://example.com/terms")
                .contact(Contact().email("rmz@redotter.sg"))

        oas.info(info)
        val oasConfig = SwaggerConfiguration()
                .openAPI(oas)
                .prettyPrint(true)
                .resourcePackages(Stream.of("org.ostelco.membershipmgt")
                        .collect(Collectors.toSet<String>()))
        val env = environment.jersey()
        env.register(OpenApiResource()
                .openApiConfiguration(oasConfig))


        addEs2PlusDefaultFiltersAndInterceptors(env)
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            Es2plusApplication().run(*args)
        }
    }
}


// A class that will give syntactically correct, but otherwise meaningless responses.

class PlaceholderSmDpPlusService : SmDpPlusService {
    override fun getProfileStatus(iccidList: List<String>): Es2ProfileStatusResponse {
        val statuses: List<ProfileStatus> = iccidList.map { iccid -> ProfileStatus(iccid = iccid, state = "ALLOCATED") }
        return Es2ProfileStatusResponse(
                profileStatusList = statuses)
    }

    @Throws(SmDpPlusException::class)
    override fun downloadOrder(eid: String?, iccid: String?, profileType: String?): Es2DownloadOrderResponse {
        return Es2DownloadOrderResponse(eS2SuccessResponseHeader(), iccid = "01234567890123456789")
    }

    override fun confirmOrder(eid: String?, iccid: String?, smdsAddress: String?, machingId: String?, confirmationCode: String?, releaseFlag: Boolean): Es2ConfirmOrderResponse {
        return Es2ConfirmOrderResponse(eS2SuccessResponseHeader(), eid = "1234567890123456789012", matchingId = "foo", smdsAddress = "localhost")
    }

    @Throws(SmDpPlusException::class)
    override fun cancelOrder(iccid: String?, matchingId: String?, eid: String?, finalProfileStatusIndicator: String?) {
    }

    @Throws(SmDpPlusException::class)
    override fun releaseProfile(iccid: String) {
    }
}

class PlaceholderSmDpPlusCallbackService : SmDpPlusCallbackService {
    override fun handleDownloadProgressInfo(
            header: ES2RequestHeader,
            eid: String?,
            iccid: String,
            profileType: String,
            timestamp: String,
            notificationPointId: Int,
            notificationPointStatus: ES2NotificationPointStatus,
            resultData: String?,
            imei: String?) {

    }
}

