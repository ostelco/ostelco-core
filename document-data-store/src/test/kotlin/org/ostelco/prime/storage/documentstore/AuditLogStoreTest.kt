package org.ostelco.prime.storage.documentstore

import arrow.core.getOrElse
import org.junit.BeforeClass
import org.junit.Test
import org.ostelco.prime.model.Severity.INFO
import org.ostelco.prime.model.Severity.WARN
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals

class AuditLogStoreTest {

    @Test
    fun test() {
        // Create 2 audit logs
        val log1 = org.ostelco.prime.model.CustomerActivity(
                timestamp = Instant.now().toEpochMilli(),
                severity = INFO,
                message = "test message 1"
        )

        val log2 = org.ostelco.prime.model.CustomerActivity(
                timestamp = Instant.now().toEpochMilli(),
                severity = WARN,
                message = "test message 2"
        )

        // store 2 logs
        DocumentDataStoreSingleton.logCustomerActivity(
                customerId = CUSTOMER_ID,
                customerActivity = log1
        )

        DocumentDataStoreSingleton.logCustomerActivity(
                customerId = CUSTOMER_ID,
                customerActivity = log2
        )

        // fetch all stored tokens
        assertEquals(
                setOf(log1, log2),
                DocumentDataStoreSingleton.getCustomerActivityHistory(CUSTOMER_ID).getOrElse { null }?.toSet()
        )
    }

    companion object {

        private val CUSTOMER_ID = UUID.randomUUID().toString()

        @JvmStatic
        @BeforeClass
        fun setup() {
            ConfigRegistry.config = Config(storeType = "inmemory-emulator")
        }
    }
}