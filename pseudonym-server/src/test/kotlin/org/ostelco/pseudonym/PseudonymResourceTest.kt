package org.ostelco.pseudonym

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.bigquery.BigQuery
import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import org.mockito.Mockito.mock
import org.ostelco.prime.model.ActiveMsisdnPseudonyms
import org.ostelco.prime.model.MsisdnPseudonymEntity
import org.ostelco.pseudonym.resources.PseudonymResource
import org.ostelco.pseudonym.service.PseudonymizerServiceSingleton
import javax.ws.rs.core.Response.Status
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Class for unit testing PseudonymResource.
 */
class PseudonymResourceTest {

    private val pathForGet = "/pseudonym/get"
    private val pathForCurrent = "/pseudonym/current"
    private val pathForActive = "/pseudonym/active"
    private val pathForFind = "/pseudonym/find"
    private val pathForDelete = "/pseudonym/delete"
    private val testMsisdn1 = "4790303333"
    private val testMsisdn2 = "4790309999"

    companion object {

        init {
            ConfigRegistry.config = PseudonymServerConfig()
                    .apply { this.datastoreType = "inmemory-emulator" }
            PseudonymizerServiceSingleton.init(env = null, bq = mock(BigQuery::class.java))
        }

        @ClassRule
        @JvmField
        val resources: ResourceTestRule? = ResourceTestRule.builder()
                .addResource(PseudonymResource())
                .build()
    }

    private val mapper = jacksonObjectMapper()

    /**
     * Test what happens when parameter is not given
     */
    @Test
    fun testPseudonymResourceForMissingParameter() {

        val statusCode = resources
                ?.target("$pathForCurrent/")
                ?.request()
                ?.get()
                ?.status ?: -1

        assertEquals(Status.NOT_FOUND.statusCode, statusCode)
    }

    /**
     * Test a normal request will all parameters
     */
    @Test
    fun testCurrentPseudonym() {
        val statusCode = resources
                ?.target("$pathForCurrent/$testMsisdn1")
                ?.request()
                ?.get()
                ?.status ?: -1

        assertEquals(Status.OK.statusCode, statusCode)
    }

    /**
     * Test get pseudonym for a timestamp
     */
    @Test
    fun testGetPseudonym() {

        lateinit var msisdnPseudonymEntity:MsisdnPseudonymEntity
        run {
            val result = resources
                    ?.target("$pathForCurrent/$testMsisdn1")
                    ?.request()
                    ?.get()
            assertNotNull(result)
            if (result == null) return
            assertEquals(Status.OK.statusCode, result.status)
            val json = result.readEntity(String::class.java)
            msisdnPseudonymEntity = mapper.readValue(json)
            assertEquals(testMsisdn1, msisdnPseudonymEntity.msisdn)
        }

        run {
            val result = resources
                    ?.target("$pathForGet/$testMsisdn1/${msisdnPseudonymEntity.start}")
                    ?.request()
                    ?.get()
            assertNotNull(result)
            if (result == null) return
            assertEquals(Status.OK.statusCode, result.status)
            val json = result.readEntity(String::class.java)
            val pseudonymEntity2 = mapper.readValue<MsisdnPseudonymEntity>(json)
            assertEquals(msisdnPseudonymEntity.pseudonym, pseudonymEntity2.pseudonym)
        }
    }

    /**
     * Test get pseudonym for a timestamp
     */
    @Test
    fun testActivePseudonyms() {

        lateinit var msisdnPseudonymEntity:MsisdnPseudonymEntity
        run {
            val result = resources
                    ?.target("$pathForCurrent/$testMsisdn1")
                    ?.request()
                    ?.get()
            assertNotNull(result)
            if (result == null) return
            assertEquals(Status.OK.statusCode, result.status)
            val json = result.readEntity(String::class.java)
            msisdnPseudonymEntity = mapper.readValue(json)
            assertEquals(testMsisdn1, msisdnPseudonymEntity.msisdn)
        }

        run {
            val result = resources
                    ?.target("$pathForActive/$testMsisdn1")
                    ?.request()
                    ?.get()
            assertNotNull(result)
            if (result == null) return
            assertEquals(Status.OK.statusCode, result.status)
            val json = result.readEntity(String::class.java)
            // This is how the client will recieve the output.
            val mapOfPseudonyms: Map<String, MsisdnPseudonymEntity> = mapper.readValue(json)
            val current = mapOfPseudonyms["current"]
            val next = mapOfPseudonyms["next"]
            assertNotNull(current)
            assertNotNull(next)
            if (current != null && next != null) {
                assertEquals(current.pseudonym, msisdnPseudonymEntity.pseudonym)
                assertEquals(current.end + 1, next.start)
            }
        }
    }

    /**
     * Test get pseudonym for a timestamp
     */
    @Test
    fun testActivePseudonymUsingModel() {

        lateinit var msisdnPseudonymEntity:MsisdnPseudonymEntity
        run {
            val result = resources
                    ?.target("$pathForCurrent/$testMsisdn1")
                    ?.request()
                    ?.get()
            assertNotNull(result)
            if (result == null) return
            assertEquals(Status.OK.statusCode, result.status)
            val json = result.readEntity(String::class.java)
            msisdnPseudonymEntity = mapper.readValue(json)
            assertEquals(testMsisdn1, msisdnPseudonymEntity.msisdn)
        }

        run {
            val result = resources
                    ?.target("$pathForActive/$testMsisdn1")
                    ?.request()
                    ?.get()
            assertNotNull(result)
            if (result == null) return
            assertEquals(Status.OK.statusCode, result.status)
            val json = result.readEntity(String::class.java)
            val active = mapper.readValue<ActiveMsisdnPseudonyms>(json)
            assertEquals(active.current.pseudonym, msisdnPseudonymEntity.pseudonym)
            assertEquals(active.current.end + 1, active.next.start)
        }
    }

    /**
     * Test a finding a pseudonym
     */
    @Test
    fun testFindPseudonym() {

        lateinit var msisdnPseudonymEntity:MsisdnPseudonymEntity
        run {
            val result = resources
                    ?.target("$pathForCurrent/$testMsisdn1")
                    ?.request()
                    ?.get()
            assertNotNull(result)
            if (result == null) return
            assertEquals(Status.OK.statusCode, result.status)
            val json = result.readEntity(String::class.java)
            msisdnPseudonymEntity = mapper.readValue(json)
            assertEquals(testMsisdn1, msisdnPseudonymEntity.msisdn)
        }

        run {
            val result = resources
                    ?.target("$pathForFind/${msisdnPseudonymEntity.pseudonym}")
                    ?.request()
                    ?.get()
            assertNotNull(result)
            if (result == null) return
            assertEquals(Status.OK.statusCode, result.status)
            val json = result.readEntity(String::class.java)
            val pseudonymEntity2 = mapper.readValue<MsisdnPseudonymEntity>(json)
            assertEquals(testMsisdn1, pseudonymEntity2.msisdn)
        }
    }

    /**
     * Test deleting all pseudonyms for a msisdn
     */
    @Test
    fun testDeletePseudonym() {
        lateinit var msisdnPseudonymEntity:MsisdnPseudonymEntity
        run {
            val result = resources
                    ?.target("$pathForCurrent/$testMsisdn2")
                    ?.request()
                    ?.get()
            assertNotNull(result)
            if (result == null) return
            assertEquals(Status.OK.statusCode, result.status)
            val json = result.readEntity(String::class.java)
            msisdnPseudonymEntity = mapper.readValue(json)
            assertEquals(testMsisdn2, msisdnPseudonymEntity.msisdn)
        }

        run {
            val result = resources
                    ?.target("$pathForDelete/$testMsisdn2")
                    ?.request()
                    ?.delete()
            assertNotNull(result)
            if (result == null) return
            assertEquals(Status.OK.statusCode, result.status)
            val json = result.readEntity(String::class.java)
            val countMap = mapper.readValue<Map<String, Int>>(json)
            val count = countMap["count"] ?: -1
            assertTrue(count >= 1)
        }

        run {
            val result = resources
                    ?.target("$pathForFind/${msisdnPseudonymEntity.pseudonym}")
                    ?.request()
                    ?.get()
            assertNotNull(result)
            if (result == null) return
            assertEquals(Status.NOT_FOUND.statusCode, result.status)
        }
    }
}