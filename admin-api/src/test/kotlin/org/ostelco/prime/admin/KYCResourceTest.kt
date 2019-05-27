package org.ostelco.prime.admin

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.junit.Test
import org.ostelco.prime.admin.api.KYCResource
import org.ostelco.prime.model.ScanInformation
import org.ostelco.prime.model.ScanResult
import org.ostelco.prime.model.ScanStatus
import org.ostelco.prime.model.Similarity

class KYCResourceTest {

    @Test
    fun `test all correct IdentityVerification`() {
        val identityVerification = """{ "similarity":"MATCH", "validity":"TRUE"}"""
        val res = KYCResource()
        val id = res.toIdentityVerification(identityVerification)
        if (id != null) {
            Assertions.assertThat(id.similarity).isEqualTo(Similarity.MATCH)
            Assertions.assertThat(id.validity).isEqualTo(true)
        }
        Assertions.assertThat(id).isNotNull()
    }

    @Test
    fun `test validity false IdentityVerification`() {
        val identityVerification = """{ "similarity":"MATCH", "validity":"FALSE"}"""
        val res = KYCResource()
        val id = res.toIdentityVerification(identityVerification)
        if (id != null) {
            Assertions.assertThat(id.similarity).isEqualTo(Similarity.MATCH)
            Assertions.assertThat(id.validity).isEqualTo(false)
        }
        Assertions.assertThat(id).isNotNull()
    }

    @Test
    fun `test similarity no match IdentityVerification`() {
        val identityVerification = """{ "similarity":"NO_MATCH", "validity":"TRUE"}"""
        val res = KYCResource()
        val id = res.toIdentityVerification(identityVerification)
        if (id != null) {
            Assertions.assertThat(id.similarity).isEqualTo(Similarity.NO_MATCH)
            Assertions.assertThat(id.validity).isEqualTo(true)
        }
        Assertions.assertThat(id).isNotNull()
    }

    @Test
    fun `test validity small case IdentityVerification`() {
        val identityVerification = """{ "similarity":"NO_MATCH", "validity":"true"}"""
        val res = KYCResource()
        val id = res.toIdentityVerification(identityVerification)
        if (id != null) {
            Assertions.assertThat(id.similarity).isEqualTo(Similarity.NO_MATCH)
            Assertions.assertThat(id.validity).isEqualTo(true)
        }
        Assertions.assertThat(id).isNotNull()
    }

    @Test
    fun `test IdentityVerification to string`() {
        val identityVerification = """{ "similarity":"NO_MATCH", "validity":"true"}"""
        val res = KYCResource()
        val id = res.toIdentityVerification(identityVerification)
        if (id != null) {
            val result = """{"similarity":"NO_MATCH","validity":true,"reason":null,"handwrittenNoteMatches":null}"""
            Assertions.assertThat(ObjectMapper().writeValueAsString(id)).isEqualTo(result)
        }
        Assertions.assertThat(id).isNotNull()
    }

    @Test
    fun `test ScanInformation to string`() {
        val identityVerification = """{ "similarity":"NO_MATCH", "validity":"true"}"""
        val res = KYCResource()
        val id = res.toIdentityVerification(identityVerification)
        val scanResult = ScanResult(
                vendorScanReference = "7890123",
                verificationStatus = "APPROVED_VERIFIED",
                time = 123456,
                type = "PASSPORT",
                country = "NORWAY",
                firstName = "Ole",
                lastName = "Nordmann",
                dob = "1988-01-23",
                rejectReason = id)
        val scanInformation = ScanInformation(
                scanId = "123456",
                countryCode = "sg",
                status = ScanStatus.REJECTED,
                scanResult = scanResult)
        if (id != null) {
            val result = """{"scanId":"123456","countryCode":"sg","status":"REJECTED","""+
            """"scanResult":{"vendorScanReference":"7890123","verificationStatus":"APPROVED_VERIFIED","time":123456,"type":"PASSPORT","country":"NORWAY","firstName":"Ole","""+
            """"lastName":"Nordmann","dob":"1988-01-23","rejectReason":{"similarity":"NO_MATCH","validity":true,"reason":null,"handwrittenNoteMatches":null}}}"""
            Assertions.assertThat(ObjectMapper().writeValueAsString(scanInformation)).isEqualTo(result)
        }
        Assertions.assertThat(id).isNotNull()
    }
}
