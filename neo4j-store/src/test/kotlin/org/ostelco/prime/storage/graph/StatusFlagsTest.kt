package org.ostelco.prime.storage.graph

import org.junit.Test
import org.ostelco.prime.storage.graph.StatusFlag.ADDRESS_AND_PHONE_NUMBER
import org.ostelco.prime.storage.graph.StatusFlag.JUMIO
import org.ostelco.prime.storage.graph.StatusFlag.MY_INFO
import org.ostelco.prime.storage.graph.StatusFlag.NRIC_FIN
import kotlin.test.assertEquals

class StatusFlagsTest {

    @Test
    fun `test bitMapStatusFlags`() {
        // individual
        assertEquals(1, StatusFlags.bitMapStatusFlags(JUMIO))
        assertEquals(2, StatusFlags.bitMapStatusFlags(MY_INFO))
        assertEquals(4,StatusFlags.bitMapStatusFlags(NRIC_FIN))
        assertEquals(8, StatusFlags.bitMapStatusFlags(ADDRESS_AND_PHONE_NUMBER))

        // composite
        assertEquals(3, StatusFlags.bitMapStatusFlags(JUMIO, MY_INFO))

        // all
        assertEquals(15, StatusFlags.bitMapStatusFlags(JUMIO, MY_INFO, NRIC_FIN, ADDRESS_AND_PHONE_NUMBER))
    }
}