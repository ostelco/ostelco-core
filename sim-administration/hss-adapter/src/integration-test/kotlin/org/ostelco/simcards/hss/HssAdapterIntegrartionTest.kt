package org.ostelco.simcards.hss

import arrow.core.Either
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.prime.simmanager.SimManagerError
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.FixedHostPortGenericContainer

/**
 *  This test shall set up a docker test environment running a simulated HSS (of the simple type), and
 *  a test-instance of the HssGrpcService application running under junit.  We shall then
 *  use the GRPC client to connect to the HssGrpcServce and observe that it correctly
 *  translates the grpc requests into valid requests that are sent to the simulated HSS server.
 **/
class HssAdapterIntegrartionTest {


    class KFixedHostPortGenericContainer(imageName: String) :
            FixedHostPortGenericContainer<KFixedHostPortGenericContainer>(imageName)


    companion object {

        /* Port number exposed to host by the emulated HLR service. */
        private var HLR_PORT = (20_000..29_999).random()

        @JvmField
        @ClassRule
        val HLR_RULE: KFixedHostPortGenericContainer =
                KFixedHostPortGenericContainer("python:3-alpine")
                        .withFixedExposedPort(HLR_PORT, 8080)
                        .withExposedPorts(8080)
                        .withClasspathResourceMapping(
                                "hlr.py",
                                "/service.py",
                                BindMode.READ_ONLY)
                        .withCommand("python", "/service.py")


        @JvmField
        @ClassRule
        val HSS_ADAPTER_RULE = DropwizardAppRule(HssAdapterApplication::class.java,
                // "integration-test/resources/hss-adapter-config.yaml"
                ResourceHelpers.resourceFilePath("hss-adapter-config.yaml")
        )
    }

    var HSS_NAME = "Foo"
    var MSISDN = "4712345678"
    var ICCID  = "89310410106543789301"

    lateinit var hssAdapter: HssAdapterApplication

    @Before
    fun setUp() {
        hssAdapter = HSS_ADAPTER_RULE.getApplication()
    }

    @Test
    fun testTalkingToTheTestHssWithoutUsingGrpc() {
        val result : Either<SimManagerError, Unit> =
                hssAdapter.dispatcher.activate(HSS_NAME, MSISDN, ICCID)
        assertTrue(result.isRight())
    }
}