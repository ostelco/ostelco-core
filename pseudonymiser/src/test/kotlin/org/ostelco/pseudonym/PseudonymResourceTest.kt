package org.ostelco.pseudonym

import com.google.cloud.datastore.DatastoreOptions
import org.ostelco.pseudonym.resources.PseudonymResource
import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import javax.ws.rs.core.Response.Status
import kotlin.test.assertEquals
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import com.google.cloud.datastore.Datastore
import org.assertj.core.util.Compatibility
import org.junit.BeforeClass
import java.io.IOException



class PseudonymResourceTest {

    companion object {
        val helper: LocalDatastoreHelper = LocalDatastoreHelper.create(1.0)
        lateinit var datastore: Datastore

        @BeforeClass @JvmStatic fun setup() {
            System.out.println("PseudonymResourceTest companion object")

            // things to execute once and keep around for the class
            helper.start()
            datastore = helper.options.service
            System.out.println("PseudonymResourceTest companion object")

        }
    }


    @Test
    fun testPseudonymResourceForMissingHeader() {
        System.out.println("testPseudonymResourceForMissingHeader")
        val resources = ResourceTestRule.builder()
                .addResource(PseudonymResource(datastore))
                .build()
        val statusCode = resources
                ?.target("/auth/token")
                ?.request()
                ?.get()
                ?.status ?: -1

        //assertEquals(Status.INTERNAL_SERVER_ERROR.statusCode, statusCode)
    }
}