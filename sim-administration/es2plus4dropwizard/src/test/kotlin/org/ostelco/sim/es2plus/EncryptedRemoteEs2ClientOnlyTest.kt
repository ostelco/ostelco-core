package org.ostelco.sim.es2plus

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.HttpClientConfiguration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.dropwizard.testing.DropwizardTestSupport
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import org.apache.http.client.HttpClient
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import javax.validation.Valid
import javax.validation.constraints.NotNull

class EncryptedRemoteEs2ClientOnlyTest {

    // This ICCID should be reserved for testing only, and should never be used
    // for any other purpose.
    private val iccid = "8947000000000000038"


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


    private fun getState(): String {
        val profileStatus =
                client.profileStatus(iccidList = listOf(iccid))
        assertEquals(FunctionExecutionStatusType.ExecutedSuccess, profileStatus.header.functionExecutionStatus.status)
        assertEquals(1, profileStatus.profileStatusList!!.size)

        val profileStatusResponse = profileStatus.profileStatusList!![0]

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
    @Ignore
    fun handleHappyDayScenarioTowardsRemote() {

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
        val SUPPORT: DropwizardTestSupport<DummyAppUsingSmDpPlusClientConfig> = DropwizardTestSupport<DummyAppUsingSmDpPlusClientConfig>(
                DummyAppUsingSmDpPlusClient::class.java,
                "src/test/resources/config-external-smdp.yml"
        )
    }
}


class DummyAppUsingSmDpPlusClient : Application<DummyAppUsingSmDpPlusClientConfig>() {

    override fun getName(): String {
        return "Dummy, just for initialization and setting up a client"
    }

    override fun initialize(bootstrap: Bootstrap<DummyAppUsingSmDpPlusClientConfig>) {
        // TODO: application initialization
    }

    private lateinit var httpClient: HttpClient

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
    var es2plusConfig: EsTwoPlusConfig = EsTwoPlusConfig()


    /**
     * The httpClient we use to connect to other services, including
     * ES2+ services
     */
    @Valid
    @NotNull
    @JsonProperty("httpClient")
    var httpClientConfiguration: HttpClientConfiguration = HttpClientConfiguration()
}