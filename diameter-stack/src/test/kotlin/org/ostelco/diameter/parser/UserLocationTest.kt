package org.ostelco.diameter.parser

import org.junit.Test
import org.ostelco.diameter.util.DiameterUtilities
import kotlin.test.assertEquals

class UserLocationTest {

    @Test
    fun UserLocationParserNorway() {

        val norwegianUserLocation = "8242f21078bf42f210013c0403"

        val userLocation = UserLocationParser.getParsedUserLocation(DiameterUtilities().hexStringToByteArray(norwegianUserLocation))

        assertEquals("130", userLocation?.geographicLocationType)
        assertEquals("242", userLocation?.mcc)
        assertEquals("01", userLocation?.mnc)
    }

    @Test
    fun UserLocationParserMalaysia() {

        val norwegianUserLocation = "8205f261a8b705f261006a9cd1"

        val userLocation = UserLocationParser.getParsedUserLocation(DiameterUtilities().hexStringToByteArray(norwegianUserLocation))

        assertEquals("130", userLocation?.geographicLocationType)
        assertEquals("502", userLocation?.mcc)
        assertEquals("16", userLocation?.mnc)
    }
}
