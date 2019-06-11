package org.ostelco.prime.admin

import org.assertj.core.api.Assertions
import org.junit.Test
import org.ostelco.prime.admin.api.KYCResource
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
        Assertions.assertThat(id).isNotNull()
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
    fun `test validity false boolean IdentityVerification`() {
        val identityVerification = """{ "similarity":"MATCH", "validity":false}"""
        val res = KYCResource()
        val id = res.toIdentityVerification(identityVerification)
        if (id != null) {
            Assertions.assertThat(id.similarity).isEqualTo(Similarity.MATCH)
            Assertions.assertThat(id.validity).isEqualTo(false)
        }
        Assertions.assertThat(id).isNotNull()
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
            Assertions.assertThat(objectMapper.writeValueAsString(id)).isEqualTo(result)
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
            Assertions.assertThat(objectMapper.writeValueAsString(scanInformation)).isEqualTo(result)
        }
        Assertions.assertThat(id).isNotNull()
    }

    /*
idScanStatus = [ERROR]
idCheckMicroprint = [N/A]
idType = [ID_CARD]
jumioIdScanReference = [2fcc8484-3581-42c0-b7c1-5c40f0db0143]
callBackType = [NETVERIFYID]
merchantIdScanReference = [2be3fc96-317f-41c0-9713-cf21811f75c5]
verificationStatus = [ERROR_NOT_READABLE_ID]
idCheckDocumentValidation = [N/A]
idScanImage = [https://netverify.com/recognition/v1/idscan/2fcc8484-3581-42c0-b7c1-5c40f0db0143/front]
callbackDate = [2019-06-04T01:50:57.746Z]
transactionDate = [2019-06-04T01:47:40.600Z]
idCheckDataPositions = [N/A]
idCountry = [SGP]
idScanImageBackside = [https://netverify.com/recognition/v1/idscan/2fcc8484-3581-42c0-b7c1-5c40f0db0143/back]
idCheckSignature = [N/A]
rejectReason = [{"rejectReasonCode":"200","rejectReasonDescription":"NOT_READABLE_DOCUMENT","rejectReasonDetails":[{"detailsCode":"2005","detailsDescription":"DAMAGED_DOCUMENT"}]}]
livenessImages = [["https://netverify.com/api/netverify/v2/scans/2fcc8484-3581-42c0-b7c1-5c40f0db0143/images/liveness/1","https://netverify.com/api/netverify/v2/scans/2fcc8484-3581-42c0-b7c1-5c40f0db0143/images/liveness/2","https://netverify.com/api/netverify/v2/scans/2fcc8484-3581-42c0-b7c1-5c40f0db0143/images/liveness/3","https://netverify.com/api/netverify/v2/scans/2fcc8484-3581-42c0-b7c1-5c40f0db0143/images/liveness/4","https://netverify.com/api/netverify/v2/scans/2fcc8484-3581-42c0-b7c1-5c40f0db0143/images/liveness/5","https://netverify.com/api/netverify/v2/scans/2fcc8484-3581-42c0-b7c1-5c40f0db0143/images/liveness/6"]]
clientIp = [119.56.102.101]
idScanImageFace = [https://netverify.com/recognition/v1/idscan/2fcc8484-3581-42c0-b7c1-5c40f0db0143/face]
idCheckSecurityFeatures = [N/A]
firstAttemptDate = [2019-06-04T01:49:21.197Z]
idCheckHologram = [N/A]
idScanSource = [SDK]
idCheckMRZcode = [N/A]
    */

    @Test
    fun `test toScanInformation`() {

        val dataMap = mapOf(
                "idScanStatus" to "ERROR",
                "idCheckMicroprint" to "N/A",
                "idType" to "ID_CARD",
                "jumioIdScanReference" to "2fcc8484-3581-42c0-b7c1-5c40f0db0143",
                "callBackType" to "NETVERIFYID",
                "merchantIdScanReference" to "2be3fc96-317f-41c0-9713-cf21811f75c5",
                "verificationStatus" to "ERROR_NOT_READABLE_ID",
                "idCheckDocumentValidation" to "N/A",
                "idScanImage" to "https://netverify.com/recognition/v1/idscan/2fcc8484-3581-42c0-b7c1-5c40f0db0143/front",
                "callbackDate" to "2019-06-04T01:50:57.746Z",
                "transactionDate" to "2019-06-04T01:47:40.600Z",
                "idCheckDataPositions" to "N/A",
                "idCountry" to "SGP",
                "idScanImageBackside" to "https://netverify.com/recognition/v1/idscan/2fcc8484-3581-42c0-b7c1-5c40f0db0143/back",
                "idCheckSignature" to "N/A",
                "rejectReason" to """{"rejectReasonCode":"200","rejectReasonDescription":"NOT_READABLE_DOCUMENT","rejectReasonDetails":[{"detailsCode":"2005","detailsDescription":"DAMAGED_DOCUMENT"}]}""",
                "livenessImages" to """["https://netverify.com/api/netverify/v2/scans/2fcc8484-3581-42c0-b7c1-5c40f0db0143/images/liveness/1","https://netverify.com/api/netverify/v2/scans/2fcc8484-3581-42c0-b7c1-5c40f0db0143/images/liveness/2","https://netverify.com/api/netverify/v2/scans/2fcc8484-3581-42c0-b7c1-5c40f0db0143/images/liveness/3","https://netverify.com/api/netverify/v2/scans/2fcc8484-3581-42c0-b7c1-5c40f0db0143/images/liveness/4","https://netverify.com/api/netverify/v2/scans/2fcc8484-3581-42c0-b7c1-5c40f0db0143/images/liveness/5","https://netverify.com/api/netverify/v2/scans/2fcc8484-3581-42c0-b7c1-5c40f0db0143/images/liveness/6"]""",
                "clientIp" to "119.56.102.101",
                "idScanImageFace" to "https://netverify.com/recognition/v1/idscan/2fcc8484-3581-42c0-b7c1-5c40f0db0143/face",
                "idCheckSecurityFeatures" to "N/A",
                "firstAttemptDate" to "2019-06-04T01:49:21.197Z",
                "idCheckHologram" to "N/A",
                "idScanSource" to "SDK",
                "idCheckMRZcode" to "N/A"
        )
        val scanInformation = KYCResource().toScanInformation(dataMap = dataMap)
        println(scanInformation)
    }
}

