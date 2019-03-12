package org.ostelco.prime.storage.scaninfo

import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.hybrid.HybridEncryptFactory
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient
import java.io.File

/**
 * Helper class implementing encrypt method using google tink.
 */
class ScanInfoEncrypt(private val keysetFilename: String, private val masterKeyUri: String?) {
    private val keysetHandle: KeysetHandle
    init {
        if (masterKeyUri != null) {
            // Decrypt the keyset using GCP master key
            keysetHandle = KeysetHandle.read(
                    JsonKeysetReader.withFile(File(keysetFilename)),
                    GcpKmsClient().withDefaultCredentials().getAead(masterKeyUri))
        } else {
            // Use local configuration directly (only for tests)
            keysetHandle = CleartextKeysetHandle.read(
                    JsonKeysetReader.withFile(File(keysetFilename)))
        }
    }

    // Encrypt the byte array using the public key.
    fun encryptData(data: ByteArray): ByteArray {
        val hybridEncrypt = HybridEncryptFactory.getPrimitive(keysetHandle)
        return hybridEncrypt.encrypt(data, null)
    }
}
