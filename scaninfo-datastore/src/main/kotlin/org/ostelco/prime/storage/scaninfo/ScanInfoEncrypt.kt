package org.ostelco.prime.storage.scaninfo

import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.hybrid.HybridEncryptFactory
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient
import org.ostelco.prime.getLogger
import java.io.File

class ScanInfoEncrypt(val keysetFilename: String, val masterKeyUri: String?) {
    private val logger by getLogger()
    private val keysetHandle: KeysetHandle
    init {
        logger.info(" GOOGLE_APPLICATION_CREDENTIALS = ${System.getenv("GOOGLE_APPLICATION_CREDENTIALS")}")
        if (masterKeyUri != null) {
            val decryptor = GcpKmsClient().getAead(masterKeyUri)
            if (decryptor == null) {
                logger.info("Decryptor for ${masterKeyUri} failed")
            } else {
                logger.info("Decryptor for ${masterKeyUri} created successfully")
            }
            // Decrypt the keyset using GCP master key
            keysetHandle = KeysetHandle.read(
                    JsonKeysetReader.withFile(File(keysetFilename)),
                    GcpKmsClient().withDefaultCredentials().getAead(masterKeyUri))
        } else {
            // Use local configuration directly
            keysetHandle = CleartextKeysetHandle.read(
                    JsonKeysetReader.withFile(File(keysetFilename)))
        }
    }

    fun encryptData(data: ByteArray): ByteArray {
        val hybridEncrypt = HybridEncryptFactory.getPrimitive(keysetHandle)
        return hybridEncrypt.encrypt(data, null)
    }
}
