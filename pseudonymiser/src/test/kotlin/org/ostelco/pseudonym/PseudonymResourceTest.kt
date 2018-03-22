package org.ostelco.pseudonym

import org.ostelco.pseudonym.resources.PseudonymResource
import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.Test
import javax.ws.rs.core.Response.Status
import kotlin.test.assertEquals
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import com.google.cloud.datastore.Datastore
import org.junit.BeforeClass

class PseudonymResourceTest {

    companion object {
        val helper: LocalDatastoreHelper = LocalDatastoreHelper.create(1.0)
        lateinit var datastore: Datastore

        @BeforeClass @JvmStatic fun setup() {
            // things to execute once and keep around for the class
            helper.start()
            datastore = helper.options.service
        }
    }

    @Test
    fun testPseudonymResourceForMissingHeader() {
        System.out.println("testPseudonymResourceForMissingHeader")
        val resources = ResourceTestRule.builder()
                .addResource(PseudonymResource(datastore))
                .build()
        val i = 10;
        val statusCode = resources
                ?.target("/pseudonym/current/")
                ?.request()
                ?.get()
                ?.status ?: -1

        assertEquals(Status.INTERNAL_SERVER_ERROR.statusCode, statusCode)
    }
}