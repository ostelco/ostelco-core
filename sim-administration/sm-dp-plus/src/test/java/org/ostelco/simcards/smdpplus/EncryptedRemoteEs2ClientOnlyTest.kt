package org.ostelco.simcards.smdpplus

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.HttpClientConfiguration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.testing.DropwizardTestSupport
import junit.framework.Assert.*
import org.apache.http.client.HttpClient
import org.conscrypt.OpenSSLProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.ostelco.sim.es2plus.ES2PlusClient
import org.ostelco.sim.es2plus.EsTwoPlusConfig
import org.ostelco.sim.es2plus.FunctionExecutionStatusType
import org.slf4j.LoggerFactory
import java.security.Security
import javax.validation.Valid
import javax.validation.constraints.NotNull

// XXX This test should be placed in the es2plus4dropwizard library, and
//     should be run as part of testing that application, not the SM-DP-Plus application.

class EncryptedRemoteEs2ClientOnlyTest {

    val iccid = "8947000000000000038"


    private lateinit var client: ES2PlusClient

    @Before
    fun setUp() {
        SUPPORT.before()
        this.client = SUPPORT.getApplication<DummyAppUsingSmDpPlusClient>().es2plusClient
    }

    @After
    fun tearDown() {
        SUPPORT.after()
    }


    fun getState(): String {
        val profileStatus =
                client.profileStatus(iccidList = listOf(iccid))
        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, profileStatus.header.functionExecutionStatus.status)
        assertEquals(1, profileStatus.profileStatusList!!.size)

        var profileStatusResponse = profileStatus.profileStatusList!!.get(0)

        assertTrue(profileStatusResponse.iccid!!.startsWith(iccid))
        assertNotNull(profileStatusResponse.state)
        return profileStatusResponse.state!!
    }


    /**
     * Run the typical scenario we run when allocating a sim profile.
     * The only exception is the optional move to "available" if not already
     * in that state.
     */
    @Test
    fun handleHappyDayScenario() {

        if ("AVAILABLE" != getState()) {
            setStateToAvailable()
        }
        downloadProfile()
        confirmOrder()
    }

    private fun confirmOrder() {
        val confirmResponse =
                client.confirmOrder(
                        iccid = iccid,
                        releaseFlag = true)

        // This happens to be the matching ID used for everything in the test application, not a good
        // assumption for production code, but this isn't that.
        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, confirmResponse.header.functionExecutionStatus.status)

        assertEquals("RELEASED", getState())
    }

    private fun setStateToAvailable() {
        val cancelOrderResult =
                client.cancelOrder(
                        iccid = iccid,
                        finalProfileStatusIndicator = "AVAILABLE"
                )
        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, cancelOrderResult.header.functionExecutionStatus.status)


        assertEquals("AVAILABLE", getState())
    }

    private fun downloadProfile() {
        val downloadResponse = client.downloadOrder(iccid = iccid)

        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, downloadResponse.header.functionExecutionStatus.status)
        assertTrue(downloadResponse.iccid!!.startsWith(iccid))

        assertEquals("ALLOCATED", getState())
    }

    companion object {
        val SUPPORT = DropwizardTestSupport<DummyAppUsingSmDpPlusClientConfig>(
                DummyAppUsingSmDpPlusClient::class.java,
                "config-external-smdp.yml"
        )
    }
}



class DummyAppUsingSmDpPlusClient : Application<DummyAppUsingSmDpPlusClientConfig>() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getName(): String {
        return "Dummy, just for initialization and setting up a client"
    }

    override fun initialize(bootstrap: Bootstrap<DummyAppUsingSmDpPlusClientConfig>) {
        // TODO: application initialization
    }

    lateinit var httpClient: HttpClient

    lateinit var es2plusClient: ES2PlusClient

    override fun run(config: DummyAppUsingSmDpPlusClientConfig,
                     env: Environment) {

        this.httpClient = HttpClientBuilder(env).using(config.httpClientConfiguration).build(name)
        this.es2plusClient = ES2PlusClient(
                requesterId = config.es2plusConfig.requesterId,
                host = config.es2plusConfig.host,
                port = config.es2plusConfig.port,
                httpClient = httpClient)
    }


    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            Security.insertProviderAt(OpenSSLProvider(), 1)
            DummyAppUsingSmDpPlusClient().run(*args)
        }
    }
}



/**
 * Configuration class for SM-DP+ emulator.
 */
class DummyAppUsingSmDpPlusClientConfig : Configuration() {

    /**
     * Configuring how the Open API representation of the
     * served resources will be presenting itself (owner,
     * license etc.)
     */
    @Valid
    @NotNull
    @JsonProperty("es2plusClient")
    var es2plusConfig = EsTwoPlusConfig()


    /**
     * The httpClient we use to connect to other services, including
     * ES2+ services
     */
    @Valid
    @NotNull
    @JsonProperty("httpClient")
    var httpClientConfiguration = HttpClientConfiguration()
}