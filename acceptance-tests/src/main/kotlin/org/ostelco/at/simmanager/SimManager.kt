package  org.ostelco.at.simmanager

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert
import org.junit.Test
import javax.ws.rs.client.ClientBuilder

// Trello card being used to manage development of this file: https://trello.com/c/48els9Gt

class SimManager {

    /// What we want to do.
    // Upload profiles to SM-DP+ emulator.
    // Upload profiles to HSS emulator.
    // Check that both SM-DP+ and HSS can be reached
    // Check that the healtchecks for both of these connections are accurately
    //   reflecting that the connections to HSS and SM-DP+ are working.


    // Insert profiles into Prime for an HSS without preallocated profiles.
    // Run periodic task.
    // Check that the number of available tasks is within the right range.
    // Run an API invocation via prime to allocate a profile.

    // Insert profiles into prime for an HSS with preallocated profiles
    // check that the number of available tasks is within the right range
    // Run an API invocation via prime to allocate a profile.


    /// What we may want to do
    // * Establish a DBI connection into postgres to check that the data
    //   stored there is legit.  This _could_ be used to check the statuses
    //   of the tests (both pre and postconditions).

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
        assertHealthy("smdp")
    }

    @Test
    fun emptyTest() {



    }
}
