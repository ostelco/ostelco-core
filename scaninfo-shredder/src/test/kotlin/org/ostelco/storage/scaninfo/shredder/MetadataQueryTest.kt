package org.ostelco.storage.scaninfo.shredder

import com.google.cloud.datastore.DatastoreException
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.mockito.Mockito
import org.ostelco.prime.model.ScanMetadata
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Class for testing the Datastore queries.
 */
class MetadataQueryTest {

    private fun saveScanMetaData(customerId: String, countryCode: String, id: String, scanReference: String, time: Long): Boolean {
        val keyString = "$customerId-$id"
        try {
            val scanMetadata = ScanMetadata(
                    id = id,
                    scanReference = scanReference,
                    countryCode = countryCode,
                    customerId = customerId,
                    processedTime = time)
            return scanInfoShredder.scanMetadataStore.add(scanMetadata, keyString).isRight()
        } catch (e: DatastoreException) {
            return false
        }
    }

    @Test
    fun testShred() {
        var testTime = Instant.now().toEpochMilli() - (scanInfoShredder.expiryDuration) - 10000
        // Add 200 records
        for (i in 1..2000) {
            saveScanMetaData("cid1", "sgp", "id{$i}", "ref$i", testTime)
            if (i == 1100) {
                testTime = Instant.now().toEpochMilli()
            }
        }
        runBlocking {
            val deletedItemsCount = scanInfoShredder.shred()
            assertEquals(1100, deletedItemsCount, "Missing some items while scanning for items")
            val remainingItemsCount = scanInfoShredder.scanMetadataStore.fetch({ entityQueryBuilder ->
                entityQueryBuilder.setLimit(1000)
            }).size
            assertEquals(900, remainingItemsCount, "Non expected count")
        }
    }

    companion object {
        private lateinit var scanInfoShredder: ScanInfoShredder

        @JvmStatic
        @BeforeClass
        fun init() {
            File("encrypt_key_global").delete()
            val testEnvVars = Mockito.mock(EnvironmentVars::class.java)
            Mockito.`when`(testEnvVars.getVar("JUMIO_API_TOKEN")).thenReturn("")
            Mockito.`when`(testEnvVars.getVar("JUMIO_API_SECRET")).thenReturn("")
            val config = ScanInfoShredderConfig()
                    .apply { storeType = "inmemory-emulator" }
            scanInfoShredder = ScanInfoShredder(config)
            scanInfoShredder.init(testEnvVars)
        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            scanInfoShredder.scanMetadataStore.close()
        }
    }
}
