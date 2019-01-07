package org.ostelco.simcards.admin

import org.ostelco.simcards.smdpplus.SmDpPlusApplication
import io.dropwizard.testing.ResourceHelpers
import io.dropwizard.testing.junit.DropwizardAppRule
import io.dropwizard.client.JerseyClientBuilder
//import io.dropwizard.testing.ConfigOverride
import org.junit.ClassRule
import org.junit.Test
import javax.ws.rs.core.Response
import org.assertj.core.api.Assertions.assertThat
import javax.annotation.Resource

class SimAdministrationTest {

    companion object {
        @JvmField
        @ClassRule
        val SIM_MANAGER_RULE = DropwizardAppRule(SimAdministrationApplication::class.java,
                ResourceHelpers.resourceFilePath("sim-manager.yaml"))
        @JvmField
        @ClassRule
        val SM_DP_PLUS_RULE = DropwizardAppRule(SmDpPlusApplication::class.java,
                ResourceHelpers.resourceFilePath("sm-dp-plus.yaml"))
    }

    @Test
    fun test() {

        val client = JerseyClientBuilder(SIM_MANAGER_RULE.environment)
                .build("test")

        val response = client.target("http://localhost:${SIM_MANAGER_RULE.getLocalPort()}/ping")
                .request()
                .get()

        assertThat(response.status).isEqualTo(200)
    }
}
