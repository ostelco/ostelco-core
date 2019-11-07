package org.ostelco.prime.admin

import org.assertj.core.api.Assertions
import org.junit.Test
import org.ostelco.prime.admin.resources.KYCResource
import org.ostelco.prime.jsonmapper.objectMapper
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
        Assertions.assertThat(id).isNotNull
    }

    @Test
    fun `test all correct boolean IdentityVerification`() {
        val identityVerification = """{ "similarity":"MATCH", "validity":true}"""
        val res = KYCResource()
        val id = res.toIdentityVerification(identityVerification)
        if (id != null) {
            Assertions.assertThat(id.similarity).isEqualTo(Similarity.MATCH)
            Assertions.assertThat(id.validity).isEqualTo(true)
        }
        Assertions.assertThat(id).isNotNull
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
        Assertions.assertThat(id).isNotNull
    }

    @Test
    fun `test validity false boolean IdentityVerification`() {
        val identityVerification = """{ "similarity":"MATCH", "validity":false}"""
        val res = KYCResource()
        val id = res.toIdentityVerification(identityVerification)
        if (id != null) {
            Assertions.assertThat(id.similarity).isEqualTo(Similarity.MATCH)
            Assertions.assertThat(id.validity).isEqualTo(false)
        }
        Assertions.assertThat(id).isNotNull
    }

    @Test
    fun `test incomplete IdentityVerification`() {
        val identityVerification = """{ "similarity":"MATCH"}"""
        val res = KYCResource()
        val id = res.toIdentityVerification(identityVerification)
        Assertions.assertThat(id).isNull()
    }

    @Test
    fun `test similarity unknown IdentityVerification`() {
        val identityVerification = """{ "similarity":"UNKNOWN", "validity":"TRUE"}"""
        val res = KYCResource()
        val id = res.toIdentityVerification(identityVerification)
        Assertions.assertThat(id).isNull()
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
        Assertions.assertThat(id).isNotNull
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
        Assertions.assertThat(id).isNotNull
    }

    @Test
    fun `test IdentityVerification to string`() {
        val identityVerification = """{ "similarity":"NO_MATCH", "validity":"true"}"""
        val res = KYCResource()
        val id = res.toIdentityVerification(identityVerification)
        if (id != null) {
            val result = """{"similarity":"NO_MATCH","validity":true,"reason":null,"handwrittenNoteMatches":null}"""
            Assertions.assertThat(objectMapper.writeValueAsString(id)).isEqualTo(result)
        }
        Assertions.assertThat(id).isNotNull
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
                expiry = null,
                rejectReason = id)
        val scanInformation = ScanInformation(
                scanId = "123456",
                countryCode = "sg",
                status = ScanStatus.REJECTED,
                scanResult = scanResult)
        if (id != null) {
            val result = """{"scanId":"123456","countryCode":"sg","status":"REJECTED","""+
            """"scanResult":{"vendorScanReference":"7890123","verificationStatus":"APPROVED_VERIFIED","time":123456,"type":"PASSPORT","country":"NORWAY","firstName":"Ole","""+
            """"lastName":"Nordmann","dob":"1988-01-23","expiry":null,"rejectReason":{"similarity":"NO_MATCH","validity":true,"reason":null,"handwrittenNoteMatches":null}}}"""
            Assertions.assertThat(objectMapper.writeValueAsString(scanInformation)).isEqualTo(result)
        }
        Assertions.assertThat(id).isNotNull
    }
}
