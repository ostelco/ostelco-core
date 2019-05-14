package org.ostelco.prime.storage.firebase

import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.ostelco.prime.model.ApplicationToken
import org.ostelco.prime.storage.DocumentStore
import java.lang.Thread.sleep
import java.util.*

class FbStorageTest {

    private lateinit var storage: DocumentStore

    private lateinit var prids: MutableCollection<String>

    @Before
    fun setUp() {
        initFirebaseConfigRegistry()
        this.storage = FirebaseStorage()

        sleep(MILLIS_TO_WAIT_WHEN_STARTING_UP.toLong())
        this.prids = ArrayList()
    }

    @Test
    fun addApplicationNotificationTokenTest() {

        val token = "ThisIsTheToken"
        val applicationId = "thisIsTheApplicationId"
        val tokenType = "FCM"

        val applicationToken = ApplicationToken(
                token = token,
                applicationID = applicationId,
                tokenType = tokenType)

        assertTrue(storage.addNotificationToken(CUSTOMER_ID, applicationToken))
        val reply = storage.getNotificationToken(CUSTOMER_ID, applicationId)
        Assert.assertNotNull(reply)
        Assert.assertEquals(reply?.token, token)
        Assert.assertEquals(reply?.applicationID, applicationId)
        Assert.assertEquals(reply?.tokenType, tokenType)

        Assert.assertEquals(storage.getNotificationTokens(CUSTOMER_ID).size, 1)

        assertTrue(storage.removeNotificationToken(CUSTOMER_ID, applicationId))
        Assert.assertEquals(storage.getNotificationTokens(CUSTOMER_ID).size, 0)
    }

    companion object {

        private val CUSTOMER_ID = UUID.randomUUID().toString()

        private const val MILLIS_TO_WAIT_WHEN_STARTING_UP = 3000
    }
}
