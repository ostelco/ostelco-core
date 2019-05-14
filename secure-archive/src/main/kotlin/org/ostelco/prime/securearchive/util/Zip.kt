package org.ostelco.prime.securearchive.util

import arrow.core.Either
import org.ostelco.prime.storage.NotCreatedError
import org.ostelco.prime.storage.StoreError
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Zip {

    /**
     * Creates the zip file
     */
    fun generateZipFile(fileName: String, dataMap: Map<String, ByteArray>): Either<StoreError, ByteArray> {
        val outputStream = ByteArrayOutputStream()
        val zos = ZipOutputStream(BufferedOutputStream(outputStream))
        try {
            dataMap.forEach { (name, data) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
            zos.finish()
        } catch (e: IOException) {
            return Either.left(NotCreatedError(type = "ZIP", id = fileName))
        } finally {
            zos.close()
        }
        return Either.right(outputStream.toByteArray())
    }
}