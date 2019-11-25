package org.ostelco.prime.ocs.parser

import org.junit.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class UserLocationTest {

    @Test
    fun userLocationParserNorway() {

        val locationHexString = "8242f21078bf42f210013c0403"

        val userLocation = UserLocationParser.getParsedUserLocation(locationHexString.hexStringToByteArray())

        assertEquals("130", userLocation?.geographicLocationType)
        assertEquals("242", userLocation?.mcc)
        assertEquals("01", userLocation?.mnc)
    }

    @Test
    fun userLocationParserMalaysia() {

        val locationHexString = "8205f261a8b705f261006a9cd1"

        val userLocation = UserLocationParser.getParsedUserLocation(locationHexString.hexStringToByteArray())

        assertEquals("130", userLocation?.geographicLocationType)
        assertEquals("502", userLocation?.mcc)
        assertEquals("16", userLocation?.mnc)
    }

    @Test
    fun userLocationParserBrazil() {

        val locationHexString = "8227f401a8b705f261006a9cd1"

        val userLocation = UserLocationParser.getParsedUserLocation(locationHexString.hexStringToByteArray())

        assertEquals("130", userLocation?.geographicLocationType)
        assertEquals("724", userLocation?.mcc)
        assertEquals("10", userLocation?.mnc)
    }

    @Test
    fun userLocationParserCanada() {

        val locationHexString = "8203225628b705f261006a9cd1"

        val userLocation = UserLocationParser.getParsedUserLocation(locationHexString.hexStringToByteArray())

        assertEquals("130", userLocation?.geographicLocationType)
        assertEquals("302", userLocation?.mcc)
        assertEquals("652", userLocation?.mnc)
    }
}

private fun String.hexStringToByteArray(): ByteArray {
    val len = this.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        data[i / 2] = ((Character.digit(this[i], 16) shl 4)
                + Character.digit(this[i + 1], 16)).toByte()
        i += 2
    }
    return data
}
