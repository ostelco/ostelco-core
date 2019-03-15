package org.ostelco.simcards.hss

import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.simcards.admin.SimAdministrationTest
import org.testcontainers.containers.BindMode

/**
 *  This test shall set up a docker test environment running a simulated HSS (of the simple type), and
 *  a test-instance of the HssGrpcService application running under junit.  We shall then
 *  use the GRPC client to connect to the HssGrpcServce and observe that it correctly
 *  translates the grpc requests into valid requests that are sent to the simulated HSS server.
 **/


class SimAdministrationTest {

    companion object {

        /* Port number exposed to host by the emulated HLR service. */
        private var HLR_PORT = (20_000..29_999).random()

        @JvmField
        @ClassRule
        val HLR_RULE: SimAdministrationTest.KFixedHostPortGenericContainer =
                SimAdministrationTest.KFixedHostPortGenericContainer("python:3-alpine")
                .withFixedExposedPort(HLR_PORT, 8080)
                .withExposedPorts(8080)
                .withClasspathResourceMapping("hlr.py", "/service.py",
                        BindMode.READ_ONLY)
                .withCommand("python", "/service.py")


        @JvmField
        @ClassRule
        val SM_DP_PLUS_RULE = DropwizardAppRule(HssAdapterApplication::class.java,
                ResourceHelpers.resourceFilePath("hss-adapter-config.yaml"))
    }


    @Test
    fun testRoundtrip() {
        // Just passing this test will be a victory, since it means that the
        // fixtures are starting without protest.
    }
}