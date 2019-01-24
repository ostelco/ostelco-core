package org.ostelco.prime.storage.scaninfo

import org.junit.AfterClass
import org.junit.BeforeClass
import org.mockito.Mockito
import org.ostelco.prime.model.JumioScanData
import java.io.File
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ScanInfoStoreTest {

    @BeforeTest
    fun clear() {

    }

    @Test
    fun `test - add check store`() {
        val subscriberId= "test@example.com"
        val vendorData: MultivaluedMap<String, String> = MultivaluedHashMap<String, String>()
        val scanId = "scanid1"
        val imgUrl = "https://www.gstatic.com/webp/gallery3/1.png"
        val imgUrl2 = "https://www.gstatic.com/webp/gallery3/2.png"
        vendorData.add(JumioScanData.SCAN_ID.s, scanId)
        vendorData.add(JumioScanData.SCAN_IMAGE.s, imgUrl)
        vendorData.add(JumioScanData.SCAN_IMAGE_BACKSIDE.s, imgUrl2)

        ScanInformationStoreSingleton.upsertVendorScanInformation(subscriberId, vendorData)
        val savedRecord = ScanInformationStoreSingleton.__getVendorScanInformation(subscriberId, scanId)
        assert(savedRecord.isRight())
        savedRecord.map { zip ->
            val details = zip.nextEntry
            assertEquals("postdata.json", details.name)
            val image = zip.nextEntry
            assertEquals("id.png", image.name)
            val imageBackside = zip.nextEntry
            assertEquals("id_backside.png", imageBackside.name)
        }
        File("$scanId.zip").delete()
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun init() {
            val testEnvVars = Mockito.mock(EnvironmentVars::class.java)
            Mockito.`when`(testEnvVars.getVar("JUMIO_API_TOKEN")).thenReturn("")
            Mockito.`when`(testEnvVars.getVar("JUMIO_API_SECRET")).thenReturn("")
            Mockito.`when`(testEnvVars.getVar("SCANINFO_STORAGE_BUCKET")).thenReturn("")
            ConfigRegistry.config = ScanInfoConfig()
                    .apply { this.storeType = "emulator" }
            ScanInformationStoreSingleton.init(null, testEnvVars)
        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            ScanInformationStoreSingleton.cleanup()
        }

    }
}