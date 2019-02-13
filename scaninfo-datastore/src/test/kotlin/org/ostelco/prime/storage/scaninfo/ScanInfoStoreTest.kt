package org.ostelco.prime.storage.scaninfo

import arrow.core.Either
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetWriter
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.hybrid.HybridDecryptFactory
import com.google.crypto.tink.hybrid.HybridKeyTemplates
import org.junit.AfterClass
import org.junit.BeforeClass
import org.mockito.Mockito
import org.ostelco.prime.model.JumioScanData
import org.ostelco.prime.model.VendorScanData
import org.ostelco.prime.storage.NotCreatedError
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.zip.ZipInputStream
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
        vendorData.add(JumioScanData.JUMIO_SCAN_ID.s, scanId)
        vendorData.add(JumioScanData.SCAN_IMAGE.s, imgUrl)
        vendorData.add(JumioScanData.SCAN_IMAGE_BACKSIDE.s, imgUrl2)
        vendorData.addAll(JumioScanData.SCAN_LIVENESS_IMAGES.s, listOf(imgUrl, imgUrl2))

        ScanInformationStoreSingleton.upsertVendorScanInformation(subscriberId, "global", vendorData)
        val savedFile = ScanInformationStoreSingleton.__getVendorScanInformationFile(subscriberId, "global", scanId)
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
    }

    companion object {
        private lateinit var privateKeysetHandle:KeysetHandle

        @JvmStatic
        @BeforeClass
        fun init() {
            File("encrypt_key_global").delete()
            val testEnvVars = Mockito.mock(EnvironmentVars::class.java)
            Mockito.`when`(testEnvVars.getVar("JUMIO_API_TOKEN")).thenReturn("")
            Mockito.`when`(testEnvVars.getVar("JUMIO_API_SECRET")).thenReturn("")
            Mockito.`when`(testEnvVars.getVar("SCANINFO_STORAGE_BUCKET")).thenReturn("")
            ConfigRegistry.config = ScanInfoConfig()
                    .apply { this.storeType = "emulator" }
            ScanInformationStoreSingleton.init(null, testEnvVars)
            privateKeysetHandle = KeysetHandle.generateNew(HybridKeyTemplates.ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM)
            val publicKeysetHandle = privateKeysetHandle.publicKeysetHandle
            val keysetFilename = "encrypt_key_global"
            CleartextKeysetHandle.write(publicKeysetHandle, JsonKeysetWriter.withFile(File(keysetFilename)))
        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            File("encrypt_key_global").delete()
        }

    }
}