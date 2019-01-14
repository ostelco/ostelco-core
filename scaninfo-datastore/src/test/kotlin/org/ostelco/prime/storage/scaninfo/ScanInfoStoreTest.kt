package org.ostelco.prime.storage.scaninfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.datastore.Blob
import org.junit.AfterClass
import org.junit.BeforeClass
import org.mockito.Mockito
import org.ostelco.prime.model.JumioScanData
import org.ostelco.prime.model.VendorScanInformation
import java.lang.Thread.sleep
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScanInfoStoreTest {

    @BeforeTest
    fun clear() {

    }

    @Test
    fun `test - add check store`() {
        val subscriberId= "test@example.com"
        val vendorData: MultivaluedMap<String, String> = MultivaluedHashMap<String, String>()
        val scanId = "id1"
        val imgUrl = "https://www.gstatic.com/webp/gallery3/1.png"
        val imgUrl2 = "https://www.gstatic.com/webp/gallery3/2.png"
        vendorData.add(JumioScanData.SCAN_ID.s, scanId)
        vendorData.add(JumioScanData.SCAN_IMAGE.s, imgUrl)
        vendorData.add(JumioScanData.SCAN_IMAGE_BACKSIDE.s, imgUrl2)

        ScanInformationStoreSingleton.upsertVendorScanInformation(subscriberId, vendorData)
        val savedRecord = ScanInformationStoreSingleton.__getVendorScanInformation(subscriberId)
        assert(savedRecord.isRight())
        savedRecord.map { vendorScanInformation ->
            assertEquals(scanId, vendorScanInformation.scanId)
            JumioHelper.downloadFileAsBlob(imgUrl, "", "").map {
                assert(vendorScanInformation.scanImage != null)
                assertEquals(it.first, vendorScanInformation.scanImage!!, "Different image")
            }
        }
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun init() {
            val testEnvVars = Mockito.mock(EnvironmentVars::class.java)
            Mockito.`when`(testEnvVars.getVar("JUMIO_API_TOKEN")).thenReturn("b")
            Mockito.`when`(testEnvVars.getVar("JUMIO_API_SECRET")).thenReturn("")

            ConfigRegistry.config = ScanInfoConfig()
                    .apply { this.datastoreType = "inmemory-emulator" }
            ScanInformationStoreSingleton.init(null, testEnvVars)
        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            ScanInformationStoreSingleton.cleanup()
        }

    }
}