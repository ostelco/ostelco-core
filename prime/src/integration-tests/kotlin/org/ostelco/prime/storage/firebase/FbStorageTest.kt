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
    @Throws(InterruptedException::class)
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

        assertTrue(storage.addNotificationToken(MSISDN, applicationToken))
        val reply = storage.getNotificationToken(MSISDN, applicationId)
        Assert.assertNotNull(reply)
        Assert.assertEquals(reply?.token, token)
        Assert.assertEquals(reply?.applicationID, applicationId)
        Assert.assertEquals(reply?.tokenType, tokenType)

        Assert.assertEquals(storage.getNotificationTokens(MSISDN).size, 1)

        assertTrue(storage.removeNotificationToken(MSISDN, applicationId))
        Assert.assertEquals(storage.getNotificationTokens(MSISDN).size, 0)
    }

    companion object {

        private const val MSISDN = "4747116996"

        private const val MILLIS_TO_WAIT_WHEN_STARTING_UP = 3000
    }
}
