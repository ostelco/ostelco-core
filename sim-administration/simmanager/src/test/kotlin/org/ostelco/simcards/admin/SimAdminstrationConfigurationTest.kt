package org.ostelco.simcards.admin



import org.junit.Assert.assertEquals
import org.junit.Test

class SimAminstrationConfigurationTest {

    var basicUrl   = "http://localhost_banana-apple.foo.bar89asdf.xom:4711"


    @Test
    fun testEs2PlusEndpointRewriting() {
        val config = ProfileVendorConfig(
                name = "Eplekake",
                es2plusEndpoint = "${basicUrl}/banana",
                requesterIdentifier = "pear",
                es9plusEndpoint = "http://boris.johnson/ticlke")

        assertEquals("${basicUrl}", config.getEndpoint())
    }


    @Test
    fun testEs2PlusEndpointWithNoRewriting() {
        val config = ProfileVendorConfig(
                name = "Eplekake",
                es2plusEndpoint = basicUrl,
                requesterIdentifier = "pear",
                es9plusEndpoint = "http://boris.johnson/tickle")

        assertEquals(basicUrl, config.getEndpoint())
    }
}
