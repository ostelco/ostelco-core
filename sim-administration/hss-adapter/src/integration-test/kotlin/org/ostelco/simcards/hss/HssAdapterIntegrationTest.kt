package org.ostelco.simcards.hss

import arrow.core.Either
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import junit.framework.Assert.*
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.prime.simmanager.SimManagerError
import org.testcontainers.containers.FixedHostPortGenericContainer

/**
 *  This test shall set up a docker test environment running a simulated HSS (of the simple type), and
 *  a test-instance of the HssGrpcService application running under junit.  We shall then
 *  use the GRPC client to connect to the HssGrpcServce and observe that it correctly
 *  translates the grpc requests into valid requests that are sent to the simulated HSS server.
 **/
class HssAdapterIntegrationTest {

    class KFixedHostPortGenericContainer(imageName: String) :
            FixedHostPortGenericContainer<KFixedHostPortGenericContainer>(imageName)

    companion object {
        @JvmField
        @ClassRule
        val MOCK_HSS_RULE = DropwizardAppRule(MockHssServer::class.java,
                ResourceHelpers.resourceFilePath("mock-hss-server-config.yaml"))


        @JvmField
        @ClassRule
        val HSS_ADAPTER_RULE = DropwizardAppRule(HssAdapterApplication::class.java,
                ResourceHelpers.resourceFilePath("hss-adapter-config.yaml")
        )
    }

    var HSS_NAME = "Foo"
    var MSISDN = "4712345678"
    var ICCID  = "89310410106543789301"

    lateinit var hssAdapter: HssAdapterApplication
    lateinit var hssApplication: MockHssServer

    lateinit var adapter: HssDispatcher

    @Before
    fun setUp() {
        hssAdapter = HSS_ADAPTER_RULE.getApplication()
        hssApplication = MOCK_HSS_RULE.getApplication()
        hssApplication.reset()
        adapter = HssGrpcAdapter("127.0.0.1", 9000)
    }

    @Test
    fun testActivationWithoutUsingGrpc() {
        assertFalse(hssApplication.isActivated(ICCID))
        val result : Either<SimManagerError, Unit> =
                hssAdapter.dispatcher.activate(hssName = HSS_NAME, iccid = ICCID, msisdn = MSISDN)
        assertTrue(result.isRight())
        assertTrue(hssApplication.isActivated(ICCID))
    }

    @Test
    fun testActivationUsingGrpc() {

        val response = adapter.activate(hssName = HSS_NAME, iccid = ICCID, msisdn = MSISDN)
        response.mapLeft { msg -> fail("Failed to activate via grpc: $msg") }
        assertTrue(hssApplication.isActivated(ICCID))
    }

    @Test
    fun testSuspensionUsingGrpc() {
        adapter.activate(hssName = HSS_NAME, iccid = ICCID, msisdn = MSISDN)
        assertTrue(hssApplication.isActivated(ICCID))
        val response = adapter.suspend(hssName = HSS_NAME, iccid = ICCID)
        response.mapLeft { msg -> fail("Failed to suspend via grpc: ${msg.description}") }
        assertTrue(!hssApplication.isActivated(ICCID))
    }

    @Test
    fun testPositiveHealthcheck() {
        assertFalse(adapter.iAmHealthy())
        HSS_ADAPTER_RULE.environment.healthChecks().runHealthChecks()
        assertTrue(adapter.iAmHealthy())
    }
}

