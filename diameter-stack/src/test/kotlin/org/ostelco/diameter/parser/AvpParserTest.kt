package org.ostelco.diameter.parser

import org.ostelco.diameter.model.PsInformation
import org.ostelco.diameter.model.ServiceUnit
import org.jdiameter.api.Avp
import org.jdiameter.api.AvpSet
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals

class AvpParserTest {

    private val VENDOR_ID_3GPP = 10415L

    @Test
    fun parsePsInformation() {
        val calledStationIdAvp = Mockito.mock(Avp::class.java)
        val sgsnMccMncAvp = Mockito.mock(Avp::class.java)

        `when`(calledStationIdAvp.utF8String).thenReturn("panacea")
        `when`(calledStationIdAvp.code).thenReturn(30)
        `when`(calledStationIdAvp.vendorId).thenReturn(0)

        `when`(sgsnMccMncAvp.utF8String).thenReturn("24201")
        `when`(sgsnMccMncAvp.code).thenReturn(Avp.GPP_SGSN_MCC_MNC)
        `when`(sgsnMccMncAvp.vendorId).thenReturn(VENDOR_ID_3GPP)

        val set = Mockito.mock(AvpSet::class.java)
        `when`(set.getAvp(30)).thenReturn(calledStationIdAvp)
        `when`(set.getAvp(Avp.GPP_SGSN_MCC_MNC)).thenReturn(sgsnMccMncAvp)

        val psInformation = AvpParser().parse(PsInformation::class, set)

        assertEquals("panacea", psInformation.calledStationId)
        assertEquals("24201", psInformation.sgsnMccMnc)
    }

    @Test
    fun parseServiceUnit() {

        val total = Mockito.mock(Avp::class.java)
        val input = Mockito.mock(Avp::class.java)
        val output = Mockito.mock(Avp::class.java)

        val set = Mockito.mock(AvpSet::class.java)

        `when`(set.getAvp(Avp.CC_TOTAL_OCTETS)).thenReturn(total)
        `when`(set.getAvp(Avp.CC_INPUT_OCTETS)).thenReturn(input)
        `when`(set.getAvp(Avp.CC_OUTPUT_OCTETS)).thenReturn(output)

        `when`(total.code).thenReturn(Avp.CC_TOTAL_OCTETS)
        `when`(total.vendorId).thenReturn(0)
        `when`(total.unsigned64).thenReturn(123)

        `when`(input.code).thenReturn(Avp.CC_INPUT_OCTETS)
        `when`(input.vendorId).thenReturn(0)
        `when`(input.unsigned64).thenReturn(456)

        `when`(output.code).thenReturn(Avp.CC_OUTPUT_OCTETS)
        `when`(output.vendorId).thenReturn(0)
        `when`(output.unsigned64).thenReturn(789)

        val serviceUnit = AvpParser().parse(ServiceUnit::class, set)

        assertEquals(123, serviceUnit.total)
        assertEquals(456, serviceUnit.input)
        assertEquals(789, serviceUnit.output)
    }
}
