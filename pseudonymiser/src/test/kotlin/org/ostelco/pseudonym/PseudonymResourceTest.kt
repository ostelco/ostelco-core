package org.ostelco.pseudonym

import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import org.ostelco.pseudonym.resources.PseudonymResource
import javax.ws.rs.core.Response.Status
import kotlin.test.assertEquals



class PseudonymResourceTest {

    companion object {

        private var datastore: Datastore

        init {
            val helper: LocalDatastoreHelper = LocalDatastoreHelper.create(1.0)
            helper.start()
            datastore = helper.options.service
        }

        @ClassRule
        @JvmField
        val resources = ResourceTestRule.builder()
                .addResource(PseudonymResource(datastore))
                .build()
    }

    @Test
    fun testPseudonymResourceForMissingHeader() {
        System.out.println("testPseudonymResourceForMissingHeader")

        val statusCode = resources
                ?.target("/pseudonym/current/")
                ?.request()
                ?.get()
                ?.status ?: -1

        assertEquals(Status.NOT_FOUND.statusCode, statusCode)
    }
}