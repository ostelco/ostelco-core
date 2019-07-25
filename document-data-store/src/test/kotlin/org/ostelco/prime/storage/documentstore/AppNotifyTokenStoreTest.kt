package org.ostelco.prime.storage.documentstore

import org.junit.BeforeClass
import org.junit.Test
import org.ostelco.prime.model.ApplicationToken
import java.util.*
import kotlin.test.assertEquals

class AppNotifyTokenStoreTest {

    @Test
    fun `test - store token`() {

        // Create 2 tokens
        val token1 = ApplicationToken(
                applicationID = "appId-${UUID.randomUUID()}",
                token = "token-${UUID.randomUUID()}",
                tokenType = "test-token"
        )

        val token2 = ApplicationToken(
                applicationID = "appId-${UUID.randomUUID()}",
                token = "token-${UUID.randomUUID()}",
                tokenType = "test-token"
        )

        // store 2 tokens
        DocumentDataStoreSingleton.addNotificationToken(
                customerId = CUSTOMER_ID,
                token = token1
        )

        DocumentDataStoreSingleton.addNotificationToken(
                customerId = CUSTOMER_ID,
                token = token2
        )

        // fetch all stored tokens
        assertEquals(
                setOf(token1, token2),
                DocumentDataStoreSingleton.getNotificationTokens(CUSTOMER_ID).toSet()
        )

        // fetch token using id
        assertEquals(
                token1,
                DocumentDataStoreSingleton.getNotificationToken(CUSTOMER_ID, token1.applicationID)
        )

        // Created 3rd token as copy of 1st token with same id but diff contents
        val token3 = token1.copy(token = "token-${UUID.randomUUID()}")

        // overwrite 1st token by 3rd token
        DocumentDataStoreSingleton.addNotificationToken(CUSTOMER_ID, token3)

        // fetch all stored tokens
        assertEquals(
                setOf(token3, token2),
                DocumentDataStoreSingleton.getNotificationTokens(CUSTOMER_ID).toSet()
        )

        // fetch token using id. Note that 1st and 3rd token share same id
        assertEquals(
                token3,
                DocumentDataStoreSingleton.getNotificationToken(CUSTOMER_ID, token1.applicationID)
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