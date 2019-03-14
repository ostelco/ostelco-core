package org.ostelco.simcards.hss

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment

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
 * source Prime component will then connect to the hss adapter and make requests for
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

    override fun getName(): String {
        return "hss-adapter"
    }

    override fun initialize(bootstrap: Bootstrap<HssAdapterApplicationConfiguration>?) {
        // nothing to do yet
    }

    override fun run(configuration: HssAdapterApplicationConfiguration,
                     environment: Environment) {
        // nothing to do yet
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            HssAdapterApplication().run(*args)
        }
    }
}

class HssAdapterApplicationConfiguration : Configuration() {
    /*
    @NotEmpty
    @get:JsonProperty
    @set:JsonProperty
    var template: String? = null

    @NotEmpty
    @get:JsonProperty
    @set:JsonProperty
    var defaultName = "Stranger"
    */
}