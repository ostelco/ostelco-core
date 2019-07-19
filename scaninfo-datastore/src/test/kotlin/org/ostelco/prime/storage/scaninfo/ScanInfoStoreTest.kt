package org.ostelco.prime.storage.scaninfo

import arrow.core.getOrElse
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetWriter
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.hybrid.HybridDecryptFactory
import com.google.crypto.tink.hybrid.HybridKeyTemplates
import org.junit.AfterClass
import org.junit.BeforeClass
import org.mockito.Mockito
import org.ostelco.prime.model.JumioScanData
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.time.Instant
import java.util.zip.ZipInputStream
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ScanInfoStoreTest {

    @Test
    fun `test - add check store`() {
        val customerId = "test@example.com"
        val vendorData: MultivaluedMap<String, String> = MultivaluedHashMap<String, String>()
        val scanId = "scanid1"
        val scanReference = "scanidref1"
        val imgUrl = "https://www.gstatic.com/webp/gallery3/1.png"
        val imgUrl2 = "https://www.gstatic.com/webp/gallery3/2.png"
        vendorData.add(JumioScanData.SCAN_ID.s, scanId)
        vendorData.add(JumioScanData.JUMIO_SCAN_ID.s, scanReference)
        vendorData.add(JumioScanData.SCAN_IMAGE.s, imgUrl)
        vendorData.add(JumioScanData.SCAN_IMAGE_BACKSIDE.s, imgUrl2)
        vendorData.addAll(JumioScanData.SCAN_LIVENESS_IMAGES.s, listOf(imgUrl, imgUrl2))

        ScanInformationStoreSingleton.upsertVendorScanInformation(customerId, "global", vendorData)
        val savedFile = ScanInformationStoreSingleton.__getVendorScanInformationFile("global", scanId)
        assert(savedFile.isRight())
        savedFile.map { filename ->
            val file = File(filename)
            val fis = FileInputStream(file)
            val data = ByteArray(file.length().toInt())
            fis.read(data)
            fis.close()

            val hybridDecrypt = HybridDecryptFactory.getPrimitive(privateKeysetHandle)
            val decrypted = hybridDecrypt.decrypt(data, null)

            val zip = ZipInputStream(ByteArrayInputStream(decrypted))
            val details = zip.nextEntry
            assertEquals("postdata.json", details.name)
            val image = zip.nextEntry
            assertEquals("id.png", image.name)
            val imageBackside = zip.nextEntry
            assertEquals("id_backside.png", imageBackside.name)
            File(filename).delete()
        }
        val scanMetadata = ScanInformationStoreSingleton
                .__getScanMetaData(customerId, scanId)
                .getOrElse { fail("Failed to get ScanMetaData") }

        assertEquals(scanReference, scanMetadata.scanReference)
        assertEquals("global", scanMetadata.countryCode)
        assertTrue(scanMetadata.processedTime <= Instant.now().toEpochMilli())
    }

    companion object {
        private lateinit var privateKeysetHandle: KeysetHandle

        @JvmStatic
        @BeforeClass
        fun init() {
            File("encrypt_key_global").delete()
            val testEnvVars = Mockito.mock(EnvironmentVars::class.java)
            Mockito.`when`(testEnvVars.getVar("JUMIO_API_TOKEN")).thenReturn("")
            Mockito.`when`(testEnvVars.getVar("JUMIO_API_SECRET")).thenReturn("")
            ConfigRegistry.config = ScanInfoConfig(storeType = "inmemory-emulator")
            ScanInformationStoreSingleton.init(testEnvVars)
            privateKeysetHandle = KeysetHandle.generateNew(HybridKeyTemplates.ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM)
            val publicKeysetHandle = privateKeysetHandle.publicKeysetHandle
            val keysetFilename = "encrypt_key_global"
            CleartextKeysetHandle.write(publicKeysetHandle, JsonKeysetWriter.withFile(File(keysetFilename)))
        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            File("encrypt_key_global").delete()
            ScanInformationStoreSingleton.scanMetadataStore.close()
        }
    }
}