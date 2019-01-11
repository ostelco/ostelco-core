package org.ostelco.prime.storage.scaninfo

import arrow.core.Either
import com.codahale.metrics.health.HealthCheck
import com.google.cloud.NoCredentials
import com.google.cloud.datastore.*
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import com.google.cloud.http.HttpTransportOptions
import io.dropwizard.setup.Environment
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.*
import org.ostelco.prime.storage.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.concurrent.schedule


class ScanInfoStore : ScanInformationStore by ScanInformationStoreSingleton

enum class ScanDatastore(val s: String) {
    // Property names in VendorScanInformation
    ID("scanId"),
    DETAILS("scanDetails"),
    IMAGE("scanImage"),
    IMAGE_TYPE("scanImageType"),
    IMAGEBACKSIDE("scanImageBackside"),
    IMAGEBACKSIDE_TYPE("scanImageBacksideType"),
    IMAGEFACE("scanImageFace"),
    IMAGEFACE_TYPE("scanImageFaceType"),
    // Name of the datastore type
    TYPE_NAME("VendorScanInformation")
}

object ScanInformationStoreSingleton : ScanInformationStore {

    private val logger by getLogger()
    private lateinit var datastore: Datastore
    // Used by unit tests
    private lateinit var localDatastoreHelper: LocalDatastoreHelper

    override fun upsertVendorScanInformation(subscriberId: String, vendorScanInformation: VendorScanInformation): Either<StoreError, Unit> {
        try {
            val key = datastore.newKeyFactory().setKind(ScanDatastore.TYPE_NAME.s).newKey(subscriberId)

            val entityBuilder = Entity.newBuilder(key)

            entityBuilder.set(ScanDatastore.ID.s, vendorScanInformation.scanId)
            entityBuilder.set(ScanDatastore.DETAILS.s, vendorScanInformation.scanDetails)
            entityBuilder.set(ScanDatastore.IMAGE.s, vendorScanInformation.scanImage)
            entityBuilder.set(ScanDatastore.IMAGE_TYPE.s, vendorScanInformation.scanImageType)
            if (vendorScanInformation.scanImageBackside != null) {
                entityBuilder.set(ScanDatastore.IMAGEBACKSIDE.s, vendorScanInformation.scanImageBackside)
            }
            if (vendorScanInformation.scanImageBacksideType != null) {
                entityBuilder.set(ScanDatastore.IMAGEBACKSIDE_TYPE.s, vendorScanInformation.scanImageBacksideType)
            }
            if (vendorScanInformation.scanImageFace != null) {
                entityBuilder.set(ScanDatastore.IMAGEFACE.s, vendorScanInformation.scanImageFace)
            }
            if (vendorScanInformation.scanImageFaceType != null) {
                entityBuilder.set(ScanDatastore.IMAGEFACE_TYPE.s, vendorScanInformation.scanImageFaceType)
            }
            datastore.put(entityBuilder.build())
        }
        catch (e: DatastoreException) {
            return Either.left(NotCreatedError(ScanDatastore.TYPE_NAME.s, subscriberId))
        }
        return Either.right(Unit)
    }

    override fun fetchScanImage(url: String): Either<StoreError, Pair<Blob, String>> {
        return HttpDownloadUtility.downloadFileAsBlob(url, ConfigRegistry.config.apiToken, ConfigRegistry.config.apiSecret)
    }

    fun __getVendorScanInformation(subscriberId: String): Either<StoreError, VendorScanInformation> {
        try {
            val key = datastore.newKeyFactory().setKind(ScanDatastore.TYPE_NAME.s).newKey(subscriberId)
            val entity = datastore.get(key)
            return Either.right(VendorScanInformation(
                    entity.getString(ScanDatastore.ID.s),
                    entity.getString(ScanDatastore.DETAILS.s),
                    entity.getBlob(ScanDatastore.IMAGE.s),
                    entity.getString(ScanDatastore.IMAGE_TYPE.s),
                    if (entity.contains(ScanDatastore.IMAGEBACKSIDE.s)) entity.getBlob(ScanDatastore.IMAGEBACKSIDE.s) else null,
                    if (entity.contains(ScanDatastore.IMAGEBACKSIDE_TYPE.s)) entity.getString(ScanDatastore.IMAGEBACKSIDE_TYPE.s) else null,
                    if (entity.contains(ScanDatastore.IMAGEFACE.s)) entity.getBlob(ScanDatastore.IMAGEFACE.s) else null,
                    if (entity.contains(ScanDatastore.IMAGEFACE_TYPE.s)) entity.getString(ScanDatastore.IMAGEFACE_TYPE.s) else null
            ))
        }
        catch (e: DatastoreException) {
            return Either.left(NotFoundError(ScanDatastore.TYPE_NAME.s, subscriberId))
        }
    }

    fun init(env: Environment?) {
        initDatastore(env)
    }

    fun cleanup() {
        if (ConfigRegistry.config.datastoreType == "inmemory-emulator") {
            // Stop the emulator after unit tests.
            logger.info("Stopping in-memory datastore emulator")
            localDatastoreHelper.stop()
        }
    }

    // Integration testing helper for Datastore.
    private fun initDatastore(env: Environment?) {
        datastore = when (ConfigRegistry.config.datastoreType) {
            "inmemory-emulator" -> {
                logger.info("Starting with in-memory datastore emulator")
                localDatastoreHelper = LocalDatastoreHelper.create(1.0)
                localDatastoreHelper.start()
                localDatastoreHelper.options
            }
            "emulator" -> {
                // When prime running in GCP by hosted CI/CD, Datastore client library assumes it is running in
                // production and ignore our instruction to connect to the datastore emulator. So, we are explicitly
                // connecting to emulator
                logger.info("Connecting to datastore emulator")
                DatastoreOptions
                        .newBuilder()
                        .setHost("localhost:9090")
                        .setCredentials(NoCredentials.getInstance())
                        .setTransportOptions(HttpTransportOptions.newBuilder().build())
                        .build()
            }
            else -> {
                logger.info("Created default instance of datastore client")
                DatastoreOptions
                        .newBuilder()
                        .setNamespace(ConfigRegistry.config.namespace)
                        .build()
            }
        }.service

        // health-check for datastore
        env?.healthChecks()?.register("datastore", object : HealthCheck() {
            override fun check(): Result {
                try {
                    val testKey = datastore.newKeyFactory().setKind("TestKind").newKey("testKey")
                    val testPropertyKey = "testPropertyKey"
                    val testPropertyValue = "testPropertyValue"
                    val testEntity = Entity.newBuilder(testKey).set(testPropertyKey, testPropertyValue).build()
                    datastore.put(testEntity)
                    val value = datastore.get(testKey).getString(testPropertyKey)
                    datastore.delete(testKey)
                    if (testPropertyValue != value) {
                        logger.warn("Unable to fetch test property value from datastore")
                        return Result.builder().unhealthy().build()
                    }
                    return Result.builder().healthy().build()
                } catch (e: Exception) {
                    return Result.builder().unhealthy(e).build()
                }
            }
        })
        Timer("SettingUp", false).schedule(1000) {
            _checkStore()
        }
    }

    fun _checkStore() {
        val subscriberId= "test@example.com"
        val scanId= "1xx"
        val scanDetails= "{ \"data\": \"empty\"}"
        val imageDataString = "asdfasfda";
        val imageDataType = "image/jpeg";
        val imageData: Blob = Blob.copyFrom(imageDataString.toByteArray())

        logger.info("_checkStore()")

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
            logger.info("Got the saved data: $it")
        }
    }
}

/**
 * A utility that downloads a file from a URL.
*/
object HttpDownloadUtility {
    /**
     * Retrieves the contents of a file from a URL
     */
    fun downloadFileAsBlob(fileURL: String, username: String, password: String): Either<StoreError, Pair<Blob, String>> {
        val url = URL(fileURL)
        val httpConn = url.openConnection() as HttpURLConnection
        val userpass = "$username:$password"
        val authHeader = "Basic ${Base64.getEncoder().encodeToString(userpass.toByteArray())}"
        httpConn.setRequestProperty("Authorization", authHeader)

        try {
            val responseCode = httpConn.responseCode
            // always check HTTP response code first
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val statusMessage = "$responseCode: ${httpConn.responseMessage}"
                return Either.left(FileDownloadError(fileURL, statusMessage));
            }
            val contentType = httpConn.contentType
            val inputStream = httpConn.inputStream
            val fileData = Blob.copyFrom(inputStream)
            inputStream.close()
            return Either.right(Pair(fileData, contentType))
        }
        catch (e: IOException) {
            val statusMessage = "IOException:  $e"
            return Either.left(FileDownloadError(fileURL, statusMessage))
        } finally {
            httpConn.disconnect()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val fileURL = "https://jdbc.postgresql.org/download/postgresql-9.2-1002.jdbc4.jar"
        try {
            val ret = HttpDownloadUtility.downloadFileAsBlob(fileURL, "", "")
            println(ret)
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }
}