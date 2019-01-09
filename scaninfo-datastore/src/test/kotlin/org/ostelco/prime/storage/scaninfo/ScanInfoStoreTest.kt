package org.ostelco.prime.storage.scaninfo

import com.google.cloud.datastore.Blob
import org.junit.AfterClass
import org.junit.BeforeClass
import org.ostelco.prime.model.VendorScanInformation
import java.lang.Thread.sleep
import kotlin.test.BeforeTest
import kotlin.test.Test

class ScanInfoStoreTest {

    @BeforeTest
    fun clear() {

    }

    @Test
    fun `test - add subscriber`() {
        val imageData: Blob = Blob.copyFrom("asdfasfda".toByteArray())
        ScanInformationStoreSingleton.upsertVendorScanInformation("sdfsf",
                VendorScanInformation("1", "sdfsdf", imageData, "image/jpeg", null, null, null, null))
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun init() {
            ConfigRegistry.config = Config()
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