package org.ostelco.diameter.parser

import org.junit.Test
import org.ostelco.diameter.util.DiameterUtilities
import kotlin.test.assertEquals

class UserLocationTest {

    @Test
    fun UserLocationParserNorway() {

        val locationHexString = "8242f21078bf42f210013c0403"

        val userLocation = UserLocationParser.getParsedUserLocation(DiameterUtilities().hexStringToByteArray(locationHexString))

        assertEquals("130", userLocation?.geographicLocationType)
        assertEquals("242", userLocation?.mcc)
        assertEquals("01", userLocation?.mnc)
    }

    @Test
    fun UserLocationParserMalaysia() {

        val locationHexString = "8205f261a8b705f261006a9cd1"

        val userLocation = UserLocationParser.getParsedUserLocation(DiameterUtilities().hexStringToByteArray(locationHexString))

        assertEquals("130", userLocation?.geographicLocationType)
        assertEquals("502", userLocation?.mcc)
        assertEquals("16", userLocation?.mnc)
    }

    @Test
    fun UserLocationParserBrazil() {

        val locationHexString = "8227f401a8b705f261006a9cd1"

        val userLocation = UserLocationParser.getParsedUserLocation(DiameterUtilities().hexStringToByteArray(locationHexString))

        assertEquals("130", userLocation?.geographicLocationType)
        assertEquals("724", userLocation?.mcc)
        assertEquals("10", userLocation?.mnc)
    }

    @Test
    fun UserLocationParserCanada() {

        val locationHexString = "8203225628b705f261006a9cd1"

        val userLocation = UserLocationParser.getParsedUserLocation(DiameterUtilities().hexStringToByteArray(locationHexString))

        assertEquals("130", userLocation?.geographicLocationType)
        assertEquals("302", userLocation?.mcc)
        assertEquals("652", userLocation?.mnc)
    }
}
