package org.ostelco.prime.securearchive.util

import arrow.core.Either
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import org.ostelco.prime.model.VendorScanData.TYPE_NAME
import org.ostelco.prime.storage.NotCreatedError
import org.ostelco.prime.storage.StoreError
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FileStore {

    /**
     * Upload the byte array to the given cloud storage object.
     */
    fun uploadFileToCloudStorage(bucket: String, fileName: String, data: ByteArray): Either<StoreError, String> {
        val storage = StorageOptions.getDefaultInstance().service
        val blobId = BlobId.of(bucket, fileName)
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/octet-stream").build()
        val mediaLink: String
        try {
            val blob = storage.create(blobInfo, data)
            mediaLink = blob.mediaLink
        } catch (e: StorageException) {
            return Either.left(NotCreatedError(TYPE_NAME.s, "$bucket/$fileName"))
        }
        return Either.right(mediaLink)
    }

    /**
     * Save byte array as local file, used only for testing
     */
    fun saveLocalFile(fileName: String, data: ByteArray): Either<StoreError, String> {
        try {
            FileOutputStream(File(fileName)).use { fos ->
                fos.write(data)
            }
        } catch (e: IOException) {
            return Either.left(NotCreatedError(TYPE_NAME.s, fileName))
        }
        return Either.right(fileName)
    }
}