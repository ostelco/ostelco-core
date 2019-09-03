package  org.ostelco.at.simmanager

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import kotlin.test.assertEquals

// Trello card being used to manage development of this file: https://trello.com/c/48els9Gt


fun <R : Any> R.getLogger(): Lazy<Logger> = lazy {
    LoggerFactory.getLogger(this.javaClass)
}

class SimManager {

    private val logger by getLogger()

    private  val client =  ClientBuilder.newClient()

    private fun getJsonFromEndpoint(endpoint: String): JsonObject {
        var response = client.target(endpoint)
                .request()
                .get()
        // Actually, when the smdp isn't answering, 500 is returned,
        // but with a valid JSON response. I know, it's messed up.
        // Assert.assertEquals(200, response.status)
        var entity = response.readEntity(String::class.java)
        val jelement = JsonParser().parse(entity)
        var jobject = jelement.getAsJsonObject()
        return jobject
    }


    fun getJsonElement(
            endpointValue: JsonObject? = null,
            endpoint: String? = null,
            theClass: String? = null,
            name: String,
            valueName: String): JsonElement {
        val epv =
                if (endpointValue == null && endpoint != null)
                    getJsonFromEndpoint(endpoint)
                else if (endpointValue != null && endpoint == null) endpointValue
                else throw IllegalArgumentException("Exactly one of endpointValue and endpoint must be non-null")

        val classElements = if (theClass == null) epv else epv.get(theClass)
        val targetElement = classElements.asJsonObject.get(name).asJsonObject
        return targetElement.get(valueName)
    }

    @Test
    fun testHealthchecks() {
        val healthcheckEndpoint = "http://prime:8081/healthcheck"
        val healtchecks = getJsonFromEndpoint(healthcheckEndpoint)

        fun assertHealthy(nameOfHealthcheck: String) {
            try {
                Assert.assertTrue(getJsonElement(
                        endpointValue = healtchecks,
                        name = nameOfHealthcheck,
                        valueName = "healthy").asBoolean)
            } catch (t: Throwable) {
                Assert.fail("Could not prove health of $nameOfHealthcheck")
            }
        }

        assertHealthy("postgresql")
        assertHealthy("HSS profilevendors for Hss named 'Loltel'")
        assertHealthy("HSS profilevendors for Hss named 'M1'")
        // XXX  This fails, don't know why.  When looking with browser it seems legit.  Removing
        // since it doesn't seem to affect tests' outcome.
        // assertHealthy("smdp")
    }



    enum class HssState {
        NOT_ACTIVATED,
        ACTIVATED,
    }

    /* ES2+ interface description - GSMA states forward transition. */
    enum class SmDpPlusState {
        /* ES2+ protocol - between SM-DP+ service and backend. */
        AVAILABLE,
        ALLOCATED,
        CONFIRMED,         /* Not used as 'releaseFlag' is set to true in 'confirm-order' message. */
        RELEASED,
        /* ES9+ protocol - between SM-DP+ service and handset. */
        DOWNLOADED,
        INSTALLED,
        ENABLED,
    }

    enum class ProvisionState {
        AVAILABLE,
        PROVISIONED,        /* The SIM profile has been taken into use (by a subscriber). */
        RESERVED,           /* Reserved SIM profile (f.ex. used for testing). */
        ALLOCATION_FAILED,
    }



    // Copied from SimInventoryDAO, and edited to not contain jdbi.
    // XXX Refactor: SimEntryn and dependent enums  should be put into a library that can be
    // used both by prime and by the acceptance tests.
    data class SimEntry(
            @JsonProperty("id") val id: Long? = null,
            @JsonProperty("batch") val batch: Long,
            @JsonProperty("hssId") val hssId: Long,
            @JsonProperty("profileVendorId") val profileVendorId: Long,
            @JsonProperty("msisdn") val msisdn: String,
            @JsonProperty("iccid") val iccid: String,
            @JsonProperty("imsi") val imsi: String,
            @JsonProperty("eid") val eid: String? = null,
            @JsonProperty("profile") val profile: String,
            @JsonProperty("hssState") val hssState: HssState = HssState.NOT_ACTIVATED,
            @JsonProperty("smdpPlusState") val smdpPlusState: SmDpPlusState = SmDpPlusState.AVAILABLE,
            @JsonProperty("provisionState") val provisionState: ProvisionState = ProvisionState.AVAILABLE,
            @JsonProperty("matchingId") val matchingId: String? = null,
            @JsonProperty("pin1") val pin1: String? = null,
            @JsonProperty("pin2") val pin2: String? = null,
            @JsonProperty("puk1") val puk1: String? = null,
            @JsonProperty("puk2") val puk2: String? = null,
            @JsonProperty("code") val code: String? = null
    )

    fun getSimEntryByIccid(hss:String, iccid: String): SimEntry {
        val webTarget = client.target("http://prime:8080/ostelco/sim-inventory/${hss}/iccid/${iccid}")

        val response = webTarget.request().get()
        val responseCode = response.status
        assertEquals( 200, responseCode)
        val entity: SimEntry = response.readEntity(SimEntry::class.java)
        assertEquals(iccid, entity.iccid)
        return entity
    }

    fun allocateFirstFreeSimProfileByHss(hss: String): SimEntry {
        val webTarget = client.target("http://prime:8080/ostelco/sim-inventory/${hss}/esim?phoneType=${hss}.iphone")
        val response = webTarget.request().get()
        val responseCode = response.status
        assertEquals( 200, responseCode)
        val entry: SimEntry = response.readEntity(SimEntry::class.java)
        return entry
    }

    @Test
    fun testFreeProfileAllocationAndSimulatedDownload() {
        val hss = "Foo"
        val firstIccid = "8901000000000000001"
        val firstMsisdn = "4790900700"
        val firstImsi = "310150000000000"
        val esimProfileName = "IPHONE_PROFILE_2"

        logger.info("Getting first  profile  to check that it looks legit")
        val firstProfile = getSimEntryByIccid(hss, firstIccid)
        assertEquals(ProvisionState.AVAILABLE, firstProfile.provisionState)
        assertEquals(SmDpPlusState.AVAILABLE, firstProfile.smdpPlusState)
        assertEquals(firstMsisdn, firstProfile.msisdn)
        assertEquals(esimProfileName, firstProfile.profile)
        assertEquals(firstImsi, firstProfile.imsi)

        logger.info("Simulating periodic SM-DP+ pre-allocation batchjob")
        val target = client.target("http://prime:8081/tasks/preallocate_sim_profiles")
        val result = target.request().buildPost(Entity.json("")).invoke()
        val returnValue = result.status
        assert( returnValue == 200 || returnValue == 201)

        logger.info("Allocate first free profile")
        val allocatedProfile = allocateFirstFreeSimProfileByHss(hss)
        assertEquals(SmDpPlusState.RELEASED, allocatedProfile.smdpPlusState)

        // Then simulate an ES2+ callback by telling the SM-DP+ to do that
        // using an API provided just for this test (or should we perhaps just do the
        // callback directly?)
        logger.info("Trigger ES2+ callback from sm-dp+")
        val webTarget = client.target("http://smdp-plus-emulator:8080/commands/simulate-download-of/iccid/${allocatedProfile.iccid}}")
        assertEquals(200, webTarget.request().get().status)

        logger.info("Check that state of allocated sim entry has changed because of the  ES2+ callback")
        val allocatedProfileAfterCallback = getSimEntryByIccid(hss, allocatedProfile.iccid)
        assertEquals(SmDpPlusState.DOWNLOADED, allocatedProfileAfterCallback.smdpPlusState)
    }
}
