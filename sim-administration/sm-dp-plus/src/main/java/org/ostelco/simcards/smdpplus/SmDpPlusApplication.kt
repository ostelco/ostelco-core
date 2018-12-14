package org.ostelco.simcards.smdpplus

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.client.JerseyClientConfiguration
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.ostelco.dropwizardutils.OpenapiResourceAdder.Companion.addOpenapiResourceToJerseyEnv
import org.ostelco.dropwizardutils.OpenapiResourceAdderConfig
import org.ostelco.sim.es2plus.ES2PlusIncomingHeadersFilter.Companion.addEs2PlusDefaultFiltersAndInterceptors
import org.ostelco.sim.es2plus.SmDpPlusServerResource
import org.ostelco.sim.es2plus.SmDpPlusService
import org.slf4j.LoggerFactory
import java.io.FileInputStream
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

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getName(): String {
        return "SM-DP+ implementation (partial, only for testing of sim admin service)"
    }

    override fun initialize(bootstrap: Bootstrap<SmDpPlusAppConfiguration>) {
        // TODO: application initialization
    }

    override fun run(configuration: SmDpPlusAppConfiguration,
                     environment: Environment) {

        val jerseyEnvironment = environment.jersey()

        addOpenapiResourceToJerseyEnv(jerseyEnvironment, configuration.openApi)
        addEs2PlusDefaultFiltersAndInterceptors(jerseyEnvironment)

        val simEntriesIterator = SmDpSimEntryIterator(FileInputStream(configuration.simBatchData))
        val smdpPlusService : SmDpPlusService =  SmDpPlusEmulator(simEntriesIterator)

        jerseyEnvironment.register(SmDpPlusServerResource(smDpPlus = smdpPlusService))
    }


    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SmDpPlusApplication().run(*args)
        }
    }
}


class SmDpPlusEmulator (incomingEntries: Iterator<SmDpSimEntry>):  SmDpPlusService {

    val entries:Set<SmDpSimEntry>

    init {
        val entrySet = mutableSetOf<SmDpSimEntry>()
        incomingEntries.forEach { entrySet.add(it)}
        entries = entrySet
    }

    override fun downloadOrder(eid: String?, iccid: String?, profileType: String?): String {
        TODO("not implemented")
    }

    override fun confirmOrder(eid: String, smdsAddress: String?, machingId: String?, confirmationCode: String?) {
        TODO("not implemented")
    }

    override fun cancelOrder(eid: String, iccid: String?, matchingId: String?, finalProfileStatusIndicator: String?) {
        TODO("not implemented")
    }

    override fun releaseProfile(iccid: String) {
        TODO("not implemented")
    }
}



class SmDpPlusAppConfiguration : Configuration() {
    @Valid
    @NotNull
    @JsonProperty("database")
    var database = DataSourceFactory()

    @Valid
    @NotNull
    @JsonProperty("openApi")
    var openApi = OpenapiResourceAdderConfig()


    @Valid
    @NotNull
    @JsonProperty("simBatchData")
    var simBatchData : String = ""

    @Valid
    @NotNull
    @JsonProperty
    private val httpClient = JerseyClientConfiguration()

    fun getJerseyClientConfiguration(): JerseyClientConfiguration {
        return httpClient
    }
}