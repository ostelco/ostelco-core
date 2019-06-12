package org.ostelco.tools.prime.admin.modules

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.module.PrimeModule

/**
 * Prime Module to get access to Dropwizard's Environment so that Prime Application can be shutdown gracefully.
 *
 */
@JsonTypeName("env")
class DwEnvModule : PrimeModule {

    override fun init(env: Environment) {
        Companion.env = env
    }

    companion object {
        lateinit var env: Environment
    }
}