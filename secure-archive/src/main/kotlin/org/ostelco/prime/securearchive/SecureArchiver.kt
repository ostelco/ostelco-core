package org.ostelco.prime.securearchive

import arrow.core.Either
import arrow.core.fix
import arrow.effects.IO
import arrow.core.extensions.either.monad.monad
import org.ostelco.prime.getLogger
import org.ostelco.prime.securearchive.ConfigRegistry.config
import org.ostelco.prime.securearchive.util.Encrypter.Companion.getEncrypter
import org.ostelco.prime.securearchive.util.FileStore.saveLocalFile
import org.ostelco.prime.securearchive.util.FileStore.uploadFileToCloudStorage
import org.ostelco.prime.securearchive.util.Zip.generateZipFile
import org.ostelco.prime.storage.StoreError
import java.time.Instant

class SecureArchiver : SecureArchiveService {

    private val logger by getLogger()

    override fun archiveEncrypted(
            customerId: String,
            regionCodes: Collection<String>,
            fileName: String,
            dataMap: Map<String, ByteArray>): Either<StoreError, Unit> {

        return IO {
            Either.monad<StoreError>().binding {
                val bucketName = config.storageBucket
                logger.info("Generating Plain Zip data for customerId = {}", customerId)
                val plainZipData = generateZipFile(fileName, dataMap).bind()
                (regionCodes
                        .filter(config.regions::contains)
                        + "global")
                        .map(String::toLowerCase)
                        .forEach { regionCode ->
                            logger.info("Encrypt for region: {} for customerId = {}", regionCode, customerId)
                            val zipData = getEncrypter(regionCode).encrypt(plainZipData)
                            if (bucketName.isEmpty()) {
                                val filePath = "${regionCode}_$fileName.zip.tk"
                                logger.info("No bucket set, saving file locally {}", filePath)
                                saveLocalFile(filePath, zipData).bind()
                            } else {
                                val filePath = "$customerId/${fileName}_${Instant.now()}.zip.tk"
                                val bucket = "$bucketName-$regionCode"
                                logger.info("Saving in cloud storage {} --> {}", bucket, filePath)
                                uploadFileToCloudStorage(bucket, filePath, zipData).bind()
                            }
                        }
                Unit
            }.fix()
        }.unsafeRunSync()
    }
}