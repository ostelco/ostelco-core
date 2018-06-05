package org.ostelco.pseudonym

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import io.dropwizard.testing.junit.ResourceTestRule
import org.junit.ClassRule
import org.junit.Test
import org.mockito.Mockito.mock
import org.ostelco.pseudonym.resources.PseudonymEntity
import org.ostelco.pseudonym.resources.PseudonymResource
import org.ostelco.pseudonym.utils.WeeklyBounds
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
    private val pathForFind= "/pseudonym/find"
    private val pathForDelete= "/pseudonym/delete"
    private val testMsisdn1 = "4790303333"
    private val testMsisdn2 = "4790309999"

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
                .addResource(PseudonymResource(datastore, WeeklyBounds(), mock(BigQuery::class.java)))
                .build()
    }
    val mapper = jacksonObjectMapper()
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

        var result = resources
                ?.target("$pathForCurrent/$testMsisdn1")
                ?.request()
                ?.get()
        assertNotNull(result)
        if (result == null) return
        assertEquals(Status.OK.statusCode, result.status)
        var json = result.readEntity(String::class.java)
        var pseudonymEntity = mapper.readValue<PseudonymEntity>(json)
        assertEquals(pseudonymEntity.msisdn, testMsisdn1)

        result = resources
                ?.target("$pathForGet/$testMsisdn1/${pseudonymEntity.start}")
                ?.request()
                ?.get()
        assertNotNull(result)
        if (result == null) return
        assertEquals(Status.OK.statusCode, result.status)
        json = result.readEntity(String::class.java)
        val pseudonymEntity2 = mapper.readValue<PseudonymEntity>(json)
        assertEquals(pseudonymEntity2.pseudonym, pseudonymEntity.pseudonym)
    }

    /**
     * Test get pseudonym for a timestamp
     */
    @Test
    fun testActivePseudonyms() {

        var result = resources
                ?.target("$pathForCurrent/$testMsisdn1")
                ?.request()
                ?.get()
        assertNotNull(result)
        if (result == null) return
        assertEquals(Status.OK.statusCode, result.status)
        var json = result.readEntity(String::class.java)
        var pseudonymEntity = mapper.readValue<PseudonymEntity>(json)
        assertEquals(pseudonymEntity.msisdn, testMsisdn1)

        result = resources
                ?.target("$pathForActive/$testMsisdn1")
                ?.request()
                ?.get()
        assertNotNull(result)
        if (result == null) return
        assertEquals(Status.OK.statusCode, result.status)
        json = result.readEntity(String::class.java)
        val mapOfPseudonyms:Map<String, PseudonymEntity> = mapper.readValue<Map<String, PseudonymEntity>>(json)
        val current = mapOfPseudonyms["current"]
        val next = mapOfPseudonyms["next"]
        assertNotNull(current)
        assertNotNull(next)
        if (current != null && next != null) {
            assertEquals(current.pseudonym, pseudonymEntity.pseudonym)
            assertEquals(current.end+1, next.start)
        }
    }

    /**
     * Test a finding a pseudonym
     */
    @Test
    fun testFindPseudonym() {

        var result = resources
                ?.target("$pathForCurrent/$testMsisdn1")
                ?.request()
                ?.get()
        assertNotNull(result)
        if (result == null) return
        assertEquals(Status.OK.statusCode, result.status)
        var json = result.readEntity(String::class.java)
        var pseudonymEntity = mapper.readValue<PseudonymEntity>(json)
        assertEquals(pseudonymEntity.msisdn, testMsisdn1)

        result = resources
                ?.target("$pathForFind/${pseudonymEntity.pseudonym}")
                ?.request()
                ?.get()
        assertNotNull(result)
        if (result == null) return
        assertEquals(Status.OK.statusCode, result.status)
        json = result.readEntity(String::class.java)
        pseudonymEntity = mapper.readValue<PseudonymEntity>(json)
        assertEquals(pseudonymEntity.msisdn, testMsisdn1)
    }

    /**
     * Test deleting all pseudonyms for a msisdn
     */
    @Test
    fun testDeletePseudonym() {
        var result = resources
                ?.target("$pathForCurrent/$testMsisdn2")
                ?.request()
                ?.get()
        assertNotNull(result)
        if (result == null) return
        assertEquals(Status.OK.statusCode, result.status)
        var json = result.readEntity(String::class.java)
        var pseudonymEntity = mapper.readValue<PseudonymEntity>(json)
        assertEquals(pseudonymEntity.msisdn, testMsisdn2)

        result = resources
                ?.target("$pathForDelete/$testMsisdn2")
                ?.request()
                ?.delete()
        assertNotNull(result)
        if (result == null) return
        assertEquals(Status.OK.statusCode, result.status)
        json = result.readEntity(String::class.java)
        val countMap = mapper.readValue<Map<String, Int>>(json)
        val count = countMap["count"] ?: -1
        assertTrue(count >= 1)

        result = resources
                ?.target("$pathForFind/${pseudonymEntity.pseudonym}")
                ?.request()
                ?.get()
        assertNotNull(result)
        if (result == null) return
        assertEquals(Status.NOT_FOUND.statusCode, result.status)
    }
}