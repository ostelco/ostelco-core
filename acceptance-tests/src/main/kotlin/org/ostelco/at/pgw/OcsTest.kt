package org.ostelco.at.pgw

import org.jdiameter.api.Avp
import org.jdiameter.api.Session
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.ostelco.at.common.createProfile
import org.ostelco.at.common.createSubscription
import org.ostelco.at.common.logger
import org.ostelco.at.common.randomInt
import org.ostelco.at.jersey.get
import org.ostelco.diameter.model.RequestType
import org.ostelco.diameter.test.TestClient
import org.ostelco.diameter.test.TestHelper
import org.ostelco.prime.client.model.SubscriptionStatus
import java.lang.Thread.sleep
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Integration tests for the OcsApplication. This test uses the diameter-test lib to setup a test P-GW to
 * actually send Diameter traffic on the selected DataSource to the OcsApplication. The
 * DataSource used is the one in the configuration file for this resources.
 *
 */
class OcsTest {

    private val logger by logger()

    private var testClient: TestClient? = null

    @Before
    fun setUp() {
        testClient = TestClient()
        testClient?.initStack("/")
    }

    @After
    fun tearDown() {
        testClient?.shutdown()
        testClient = null
    }

    private fun simpleCreditControlRequestInit(session: Session) {

        val client = testClient ?: fail("Test client is null")

        val request = client.createRequest(
                DEST_REALM,
                DEST_HOST,
                session
        ) ?: fail("Failed to create request")

        TestHelper.createInitRequest(request.avps, MSISDN, BUCKET_SIZE)

        client.sendNextRequest(request, session)

        waitForAnswer()

        assertEquals(2001L, client.resultCodeAvp?.integer32?.toLong())
        val resultAvps = client.resultAvps ?: fail("Missing AVPs")
        assertEquals(RequestType.INITIAL_REQUEST.toLong(), resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32.toLong())
        assertEquals(DEST_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
        assertEquals(DEST_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
        val resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
        assertEquals(2001L, resultMSCC.grouped.getAvp(Avp.RESULT_CODE).integer32.toLong())
        assertEquals(1, resultMSCC.grouped.getAvp(Avp.SERVICE_IDENTIFIER_CCA).unsigned32)
        assertEquals(10, resultMSCC.grouped.getAvp(Avp.RATING_GROUP).unsigned32)
        val granted = resultMSCC.grouped.getAvp(Avp.GRANTED_SERVICE_UNIT)
        assertEquals(BUCKET_SIZE, granted.grouped.getAvp(Avp.CC_TOTAL_OCTETS).unsigned64)
    }

    private fun simpleCreditControlRequestUpdate(session: Session) {

        val client = testClient ?: fail("Test client is null")

        val request = client.createRequest(
                DEST_REALM,
                DEST_HOST,
                session
        ) ?: fail("Failed to create request")

        TestHelper.createUpdateRequest(request.avps, MSISDN, BUCKET_SIZE, BUCKET_SIZE)

        client.sendNextRequest(request, session)

        waitForAnswer()

        assertEquals(2001L, client.resultCodeAvp?.integer32?.toLong())
        val resultAvps = client.resultAvps ?: fail("Missing AVPs")
        assertEquals(DEST_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
        assertEquals(DEST_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
        assertEquals(RequestType.UPDATE_REQUEST.toLong(), resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32.toLong())
        val resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
        assertEquals(2001L, resultMSCC.grouped.getAvp(Avp.RESULT_CODE).integer32.toLong())
        val granted = resultMSCC.grouped.getAvp(Avp.GRANTED_SERVICE_UNIT)
        assertEquals(BUCKET_SIZE, granted.grouped.getAvp(Avp.CC_TOTAL_OCTETS).unsigned64)
    }

    private fun getBalance(): Long {
        sleep(200) // wait for 200 ms for balance to be updated in db

        val subscriptionStatus: SubscriptionStatus = get {
            path = "/subscription/status"
            subscriberId = EMAIL
        }
        return subscriptionStatus.remaining
    }

    @Test
    fun simpleCreditControlRequestInitUpdateAndTerminate() {

        val client = testClient ?: fail("Test client is null")

        val session = client.createSession() ?: fail("Failed to create session")
        simpleCreditControlRequestInit(session)
        assertEquals(INITIAL_BALANCE - BUCKET_SIZE, getBalance(), message = "Incorrect balance after init")

        simpleCreditControlRequestUpdate(session)
        assertEquals(INITIAL_BALANCE - 2 * BUCKET_SIZE, getBalance(), message = "Incorrect balance after update")

        val request = client.createRequest(
                DEST_REALM,
                DEST_HOST,
                session
        ) ?: fail("Failed to create request")

        TestHelper.createTerminateRequest(request.avps, MSISDN, BUCKET_SIZE)

        client.sendNextRequest(request, session)

        waitForAnswer()

        assertEquals(2001L, client.resultCodeAvp?.integer32?.toLong())
        val resultAvps = client.resultAvps ?: fail("Missing AVPs")
        assertEquals(DEST_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
        assertEquals(DEST_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
        assertEquals(RequestType.TERMINATION_REQUEST.toLong(), resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32.toLong())
        val resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
        assertEquals(2001L, resultMSCC.grouped.getAvp(Avp.RESULT_CODE).integer32.toLong())
        assertEquals(1, resultMSCC.grouped.getAvp(Avp.SERVICE_IDENTIFIER_CCA).unsigned32)
        assertEquals(10, resultMSCC.grouped.getAvp(Avp.RATING_GROUP).unsigned32)
        val validTime = resultMSCC.grouped.getAvp(Avp.VALIDITY_TIME)
        assertEquals(86400L, validTime.unsigned32)

        assertEquals(INITIAL_BALANCE - 2 * BUCKET_SIZE, getBalance(), message = "Incorrect balance after terminate")
    }


    @Test
    fun creditControlRequestInitNoCredit() {

        val client = testClient ?: fail("Test client is null")

        val session = client.createSession() ?: fail("Failed to create session")
        val request = client.createRequest(
                DEST_REALM,
                DEST_HOST,
                session
        ) ?: fail("Failed to create request")

        TestHelper.createInitRequest(request.avps, "4333333333", BUCKET_SIZE)

        client.sendNextRequest(request, session)

        waitForAnswer()

        run {
            assertEquals(2001L, client.resultCodeAvp?.integer32?.toLong())
            val resultAvps = client.resultAvps ?: fail("Missing AVPs")
            assertEquals(DEST_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
            assertEquals(DEST_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
            assertEquals(RequestType.INITIAL_REQUEST.toLong(), resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32.toLong())
            val resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
            assertEquals(2001L, resultMSCC.grouped.getAvp(Avp.RESULT_CODE).integer32.toLong())
            assertEquals(1, resultMSCC.grouped.getAvp(Avp.SERVICE_IDENTIFIER_CCA).integer32.toLong())
            val granted = resultMSCC.grouped.getAvp(Avp.GRANTED_SERVICE_UNIT)
            assertEquals(0L, granted.grouped.getAvp(Avp.CC_TOTAL_OCTETS).unsigned64)
        }
        // There is 2 step in graceful shutdown. First OCS send terminate, then P-GW report used units in a final update

        assertEquals(INITIAL_BALANCE, getBalance(), message = "Incorrect balance after init using wrong msisdn")

        val updateRequest = client.createRequest(
                DEST_REALM,
                DEST_HOST,
                session
        ) ?: fail("Failed to create request")

        TestHelper.createUpdateRequestFinal(updateRequest.avps, "4333333333")

        client.sendNextRequest(updateRequest, session)

        waitForAnswer()

        run {
            assertEquals(2001L, client.resultCodeAvp?.integer32?.toLong())
            val resultAvps = client.resultAvps ?: fail("Missing AVPs")
            assertEquals(DEST_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
            assertEquals(DEST_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
            assertEquals(RequestType.UPDATE_REQUEST.toLong(), resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32.toLong())
            val resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
            assertEquals(2001L, resultMSCC.grouped.getAvp(Avp.RESULT_CODE).integer32.toLong())
            assertEquals(1, resultMSCC.grouped.getAvp(Avp.SERVICE_IDENTIFIER_CCA).integer32.toLong())
            val validTime = resultMSCC.grouped.getAvp(Avp.VALIDITY_TIME)
            assertEquals(86400L, validTime.unsigned32)
        }

        assertEquals(INITIAL_BALANCE, getBalance(), message = "Incorrect balance after update using wrong msisdn")
    }


    private fun waitForAnswer() {

        val client = testClient ?: fail("Test client is null")

        var i = 0
        while (!client.isAnswerReceived && i < 10) {
            i++
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                logger.error("Start Failed", e)
            }

        }
        assertEquals(true, client.isAnswerReceived)
    }

    companion object {

        private const val DEST_REALM = "loltel"
        private const val DEST_HOST = "ocs"

        private const val INITIAL_BALANCE = 100_000_000L
        private const val BUCKET_SIZE = 500L

        private lateinit var EMAIL: String
        private lateinit var MSISDN: String

        @BeforeClass
        @JvmStatic
        fun createTestUserAndSubscription() {

            EMAIL = "ocs-${randomInt()}@test.com"
            createProfile(name = "Test OCS User", email = EMAIL)

            MSISDN = createSubscription(EMAIL)
        }
    }
}
