package org.ostelco.prime.storage.scaninfo

import com.google.cloud.datastore.Blob
import org.junit.AfterClass
import org.junit.BeforeClass
import org.ostelco.prime.model.VendorScanInformation
import java.lang.Thread.sleep
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
        val scanId= "1xx"
        val scanDetails= "{ \"data\": \"empty\"}"
        val imageDataString = "asdfasfda";
        val imageDataType = "image/jpeg";
        val imageData: Blob = Blob.copyFrom(imageDataString.toByteArray())

        ScanInformationStoreSingleton.upsertVendorScanInformation(subscriberId,
                VendorScanInformation(scanId,
                        scanDetails,
                        imageData,
                        imageDataType,
                        null,
                        null,
                        null,
                        null))
        val savedRecord = ScanInformationStoreSingleton.__getVendorScanInformation(subscriberId)
        assert(savedRecord.isRight())
        savedRecord.map {
            assertEquals(imageData, it.scanImage, "Different image")
        }
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun init() {
            ConfigRegistry.config = ScanInfoConfig()
                    .apply { this.datastoreType = "inmemory-emulator" }
            ScanInformationStoreSingleton.init(env = null)
        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            ScanInformationStoreSingleton.cleanup()
        }

    }
}