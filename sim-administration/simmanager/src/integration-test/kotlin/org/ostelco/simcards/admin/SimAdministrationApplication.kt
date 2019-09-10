package org.ostelco.simcards.admin

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.dropwizard.Application
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment


/**
 * The SIM manager test application
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
class SimAdministrationApplication : Application<SimAdministrationConfiguration>() {


    private val simAdminModule =  SimAdministrationModule()

    override fun getName(): String {
        return "SIM inventory application"
    }


    override fun initialize(bootstrap: Bootstrap<SimAdministrationConfiguration>) {
        // Enables ENV variable substitution in config file.
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
                bootstrap.configurationSourceProvider,
                EnvironmentVariableSubstitutor(false)
        )
        bootstrap.objectMapper.registerModule(KotlinModule())
    }

    override fun run(config: SimAdministrationConfiguration,
                     env: Environment) {
        simAdminModule.setConfig(config)
        simAdminModule.init(env)
    }

    fun getDAO() =  simAdminModule.getDAO()

    fun triggerMetricsGeneration() {
        simAdminModule.triggerMetricsGeneration()
    }
}


