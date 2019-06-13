package org.ostelco.simcards.hss

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.HttpClientConfiguration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.ostelco.simcards.admin.HssConfig
import javax.validation.Valid
import javax.validation.constraints.NotNull


/**
 * Boilerplate entry point.
 */
fun main(args: Array<String>) = HssAdapterApplication().run(*args)

/**
 * The sim  manager will have to interface to many different Home Subscriber Module
 * instances.  Many of these will rely on proprietary libraries to interface to the
 * HSS.  We strongly believe that the majority of the Ostelco project's source code
 * should be open sourced, but it is impossible to open source something that isn't ours,
 * so we can't open source HSS libraries, and we won't.
 *
 * Instead we'll do the next best thing: We'll make it simple to create adapters
 * for these libraries and make them available to the ostelco core.
 *
 * Our strategy is to make a service, implemented by the HssAdapterApplication, that
 * will be available as an external executable, via rest  (or possibly gRPC,  not decided
 * at the time this documentation is  being written).  The "simmanager" module of the open
 * source Prime component will then connect to the hss profilevendors and make requests for
 * activation/suspension/deletion.
 *
 * This component is written in the open source project, and it contains a non-proprietary
 * implementation of a simple HSS interface.   We provide this as a template so that when
 * proprietary code is added to this application, it can be done in the same way as the
 * simple non-proprietary implementation was added.  You are however expected to do that,
 * and make your service, deploy it separately and tell the prime component where it is
 * (typically using kubernetes service lookup or something similar).
 */
class HssAdapterApplication : Application<HssAdapterApplicationConfiguration>() {

    public lateinit var dispatcher: DirectHssDispatcher

    override fun getName(): String {
        return "HSS adapter service"
    }

    override fun initialize(bootstrap: Bootstrap<HssAdapterApplicationConfiguration>) {
        bootstrap.objectMapper.registerModule(KotlinModule())
    }

    override fun run(configuration: HssAdapterApplicationConfiguration,
                     env: Environment) {

        val httpClient = HttpClientBuilder(env)
                .using(configuration.httpClient)
                .build("$name http client")

        val myHssService = ManagedHssGrpcService(
                port = 9000,
                env = env,
                httpClient = httpClient,
                configuration = configuration.hssVendors)

        this.dispatcher = myHssService.dispatcher

        env.lifecycle().manage(myHssService)
    }
}


class HssAdapterApplicationConfiguration : Configuration() {
    @Valid
    @NotNull
    @JsonProperty("hlrs")
    lateinit var hssVendors: List<HssConfig>

    @Valid
    @NotNull
    @JsonProperty("httpClient")
    val httpClient = HttpClientConfiguration()
}
