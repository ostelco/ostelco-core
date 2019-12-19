package org.ostelco.ocsgw

import OcsServer
import org.jdiameter.api.Avp
import org.jdiameter.api.AvpDataException
import org.jdiameter.api.Session
import org.junit.Assert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.ostelco.diameter.model.ReAuthRequestType
import org.ostelco.diameter.model.ReportingReason
import org.ostelco.diameter.model.RequestType
import org.ostelco.diameter.model.SessionContext
import org.ostelco.diameter.test.TestClient
import org.ostelco.diameter.test.TestHelper.addUnknownApv
import org.ostelco.diameter.test.TestHelper.createInitRequest
import org.ostelco.diameter.test.TestHelper.createInitRequestMultiRatingGroups
import org.ostelco.diameter.test.TestHelper.createTerminateRequest
import org.ostelco.diameter.test.TestHelper.createUpdateRequest
import org.slf4j.LoggerFactory
import java.io.UnsupportedEncodingException
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import javax.xml.bind.DatatypeConverter

/**
 * Tests for the OcsApplication. This will use a TestClient to
 * actually send Diameter traffic on localhost to the OcsApplication.
 */
@DisplayName("OcsApplicationTest")
class OcsApplicationTest {
    private fun simpleCreditControlRequestInit(session: Session?, requestedBucketSize: Long, expectedGrantedBucketSize: Long, ratingGroup: Int, serviceIdentifier: Int) {
        val request = client!!.createRequest(
                OCS_REALM,
                OCS_HOST,
                session!!
        )
        createInitRequest(request!!.avps, MSISDN, requestedBucketSize, ratingGroup, serviceIdentifier)
        client!!.sendNextRequest(request, session)
        waitForAnswer(session.sessionId)
        try {
            val result = client!!.getAnswer(session.sessionId)
            Assert.assertEquals(DIAMETER_SUCCESS, result!!.resultCode!!.toLong())
            val resultAvps = result.resultAvps
            Assert.assertEquals(OCS_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
            Assert.assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
            Assert.assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32)
            val resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
            Assert.assertEquals(DIAMETER_SUCCESS, resultMSCC.grouped.getAvp(Avp.RESULT_CODE).integer32.toLong())
            if (serviceIdentifier > 0) {
                Assert.assertEquals(serviceIdentifier.toLong(), resultMSCC.grouped.getAvp(Avp.SERVICE_IDENTIFIER_CCA).unsigned32)
            }
            if (ratingGroup > 0) {
                Assert.assertEquals(ratingGroup.toLong(), resultMSCC.grouped.getAvp(Avp.RATING_GROUP).unsigned32)
            }
            val granted = resultMSCC.grouped.getAvp(Avp.GRANTED_SERVICE_UNIT)
            Assert.assertEquals(expectedGrantedBucketSize, granted.grouped.getAvp(Avp.CC_TOTAL_OCTETS).unsigned64)
        } catch (e: AvpDataException) {
            LOG.error("Failed to get Result-Code", e)
        }
    }

    private fun simpleCreditControlRequestUpdate(session: Session?,
                                                 requestedBucketSize: Long,
                                                 usedBucketSize: Long,
                                                 expectedGrantedBucketSize: Long,
                                                 ratingGroup: Int,
                                                 serviceIdentifier: Int) {
        val request = client!!.createRequest(
                OCS_REALM,
                OCS_HOST,
                session!!
        )
        createUpdateRequest(request!!.avps, MSISDN, requestedBucketSize, usedBucketSize, ratingGroup, serviceIdentifier, ReportingReason.QUOTA_EXHAUSTED)
        client!!.sendNextRequest(request, session)
        waitForAnswer(session.sessionId)
        try {
            val result = client!!.getAnswer(session.sessionId)
            Assert.assertEquals(DIAMETER_SUCCESS, result!!.resultCode!!.toLong())
            val resultAvps = result.resultAvps
            Assert.assertEquals(RequestType.UPDATE_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32)
            val resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
            Assert.assertEquals(DIAMETER_SUCCESS, resultMSCC.grouped.getAvp(Avp.RESULT_CODE).integer32.toLong())
            if (serviceIdentifier > 0) {
                Assert.assertEquals(serviceIdentifier.toLong(), resultMSCC.grouped.getAvp(Avp.SERVICE_IDENTIFIER_CCA).unsigned32)
            }
            if (ratingGroup > 0) {
                Assert.assertEquals(ratingGroup.toLong(), resultMSCC.grouped.getAvp(Avp.RATING_GROUP).unsigned32)
            }
            val granted = resultMSCC.grouped.getAvp(Avp.GRANTED_SERVICE_UNIT)
            Assert.assertEquals(expectedGrantedBucketSize, granted.grouped.getAvp(Avp.CC_TOTAL_OCTETS).unsigned64)
        } catch (e: AvpDataException) {
            LOG.error("Failed to get Result-Code", e)
        }
    }

    @Test
    @DisplayName("Simple Credit-Control-Request Init Update and Terminate")
    fun simpleCreditControlRequestInitUpdateAndTerminate() {
        val ratingGroup = 10
        val serviceIdentifier = 1
        val session = client!!.createSession(object : Any() {}.javaClass.enclosingMethod.name)
        simpleCreditControlRequestInit(session, 500000L, 500000L, ratingGroup, serviceIdentifier)
        simpleCreditControlRequestUpdate(session, 400000L, 500000L, 400000L, ratingGroup, serviceIdentifier)
        val request = client!!.createRequest(
                OCS_REALM,
                OCS_HOST,
                session!!
        )
        createTerminateRequest(request!!.avps, MSISDN, 400000L, ratingGroup, 1)
        client!!.sendNextRequest(request, session)
        waitForAnswer(session.sessionId)
        try {
            val result = client!!.getAnswer(session.sessionId)
            Assert.assertEquals(DIAMETER_SUCCESS, result!!.resultCode!!.toLong())
            val resultAvps = result.resultAvps
            Assert.assertEquals(RequestType.TERMINATION_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32)
        } catch (e: AvpDataException) {
            LOG.error("Failed to get Result-Code", e)
        }
        session.release()
    }

    @Test
    @DisplayName("Simple Credit-Control-Request Init Update no Requested-Service-Units")
    fun simpleCreditControlRequestInitUpdateNoRSU() {
        val ratingGroup = 10
        val serviceIdentifier = 1
        val session = client!!.createSession(object : Any() {}.javaClass.enclosingMethod.name)
        simpleCreditControlRequestInit(session, 500000L, 500000L, ratingGroup, serviceIdentifier)
        val request = client!!.createRequest(
                OCS_REALM,
                OCS_HOST,
                session!!
        )
        // Only report usage, no request for new bucket
        createUpdateRequest(request!!.avps, MSISDN, -1L, 500000L, ratingGroup, serviceIdentifier, ReportingReason.QUOTA_EXHAUSTED)
        client!!.sendNextRequest(request, session)
        waitForAnswer(session.sessionId)
        try {
            val result = client!!.getAnswer(session.sessionId)
            Assert.assertEquals(DIAMETER_SUCCESS, result!!.resultCode!!.toLong())
            val resultAvps = result.resultAvps
            Assert.assertEquals(RequestType.UPDATE_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32)
            Assert.assertEquals(86400L, resultAvps.getAvp(Avp.VALIDITY_TIME).integer32.toLong())
        } catch (e: AvpDataException) {
            LOG.error("Failed to get Result-Code", e)
        }
        session.release()
    }

    @Test
    @DisplayName("Credit-Control-Request Init Update and Terminate No Requested-Service-Unit Set")
    fun CreditControlRequestInitUpdateAndTerminateNoRequestedServiceUnit() {
        val ratingGroup = 10
        val serviceIdentifier = -1
        val session = client!!.createSession(object : Any() {}.javaClass.enclosingMethod.name)
        simpleCreditControlRequestInit(session, 0L, 40000000L, ratingGroup, serviceIdentifier)
        simpleCreditControlRequestUpdate(session, 0L, 40000000L, 40000000L, ratingGroup, serviceIdentifier)
        val request = client!!.createRequest(
                OCS_REALM,
                OCS_HOST,
                session!!
        )
        createTerminateRequest(request!!.avps, MSISDN, 40000000L, ratingGroup, serviceIdentifier)
        client!!.sendNextRequest(request, session)
        waitForAnswer(session.sessionId)
        try {
            val result = client!!.getAnswer(session.sessionId)
            Assert.assertEquals(DIAMETER_SUCCESS, result!!.resultCode!!.toLong())
            val resultAvps = result.resultAvps
            Assert.assertEquals(RequestType.TERMINATION_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32)
        } catch (e: AvpDataException) {
            LOG.error("Failed to get Result-Code", e)
        }
        session.release()
    }

    @Test
    @DisplayName("Credit-Control-Request Multi Ratinggroups Init")
    fun creditControlRequestMultiRatingGroupsInit() {
        val session = client!!.createSession(object : Any() {}.javaClass.enclosingMethod.name)
        val request = client!!.createRequest(
                OCS_REALM,
                OCS_HOST,
                session!!
        )
        createInitRequestMultiRatingGroups(request!!.avps, MSISDN, 500000L)
        client!!.sendNextRequest(request, session)
        waitForAnswer(session.sessionId)
        try {
            val result = client!!.getAnswer(session.sessionId)
            Assert.assertEquals(DIAMETER_SUCCESS, result!!.resultCode!!.toLong())
            val resultAvps = result.resultAvps
            Assert.assertEquals(OCS_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
            Assert.assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
            Assert.assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32)
            val resultMSCCs = resultAvps.getAvps(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
            Assert.assertEquals(3, resultMSCCs.size().toLong())
            for (i in 0 until resultMSCCs.size()) {
                val mscc = resultMSCCs.getAvpByIndex(i).grouped
                Assert.assertEquals(DIAMETER_SUCCESS, mscc.getAvp(Avp.RESULT_CODE).integer32.toLong())
                val granted = mscc.getAvp(Avp.GRANTED_SERVICE_UNIT)
                Assert.assertEquals(500000L, granted.grouped.getAvp(Avp.CC_TOTAL_OCTETS).unsigned64)
                val serviceIdentifier = mscc.getAvp(Avp.SERVICE_IDENTIFIER_CCA).unsigned32.toInt()
                when (serviceIdentifier) {
                    1 -> Assert.assertEquals(10, mscc.getAvp(Avp.RATING_GROUP).unsigned32)
                    2 -> Assert.assertEquals(12, mscc.getAvp(Avp.RATING_GROUP).unsigned32)
                    4 -> Assert.assertEquals(14, mscc.getAvp(Avp.RATING_GROUP).unsigned32)
                    else -> Assert.fail("Unexpected Service-Identifier")
                }
            }
        } catch (e: AvpDataException) {
            LOG.error("Failed to get Result-Code", e)
        }
    }

    @Test
    @DisplayName("test AVP not in Diameter dictionary")
    fun testUnknownAVP() {
        val ratingGroup = 10
        val serviceIdentifier = 1
        val session = client!!.createSession(object : Any() {}.javaClass.enclosingMethod.name)
        val request = client!!.createRequest(
                OCS_REALM,
                OCS_HOST,
                session!!
        )
        createInitRequest(request!!.avps, MSISDN, 500000L, ratingGroup, serviceIdentifier)
        addUnknownApv(request.avps)
        client!!.sendNextRequest(request, session)
        waitForAnswer(session.sessionId)
        try {
            val result = client!!.getAnswer(session.sessionId)
            Assert.assertEquals(DIAMETER_SUCCESS, result!!.resultCode!!.toLong())
            val resultAvps = result.resultAvps
            Assert.assertEquals(OCS_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
            Assert.assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
            Assert.assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32)
            val resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
            Assert.assertEquals(DIAMETER_SUCCESS, resultMSCC.grouped.getAvp(Avp.RESULT_CODE).integer32.toLong())
            Assert.assertEquals(serviceIdentifier.toLong(), resultMSCC.grouped.getAvp(Avp.SERVICE_IDENTIFIER_CCA).unsigned32)
            Assert.assertEquals(ratingGroup.toLong(), resultMSCC.grouped.getAvp(Avp.RATING_GROUP).unsigned32)
            val granted = resultMSCC.grouped.getAvp(Avp.GRANTED_SERVICE_UNIT)
            Assert.assertEquals(500000L, granted.grouped.getAvp(Avp.CC_TOTAL_OCTETS).unsigned64)
        } catch (e: AvpDataException) {
            LOG.error("Failed to get Result-Code", e)
        }
    }

    @Test
    @DisplayName("Test no MSCC  in CCR-U")
    fun testNoMsccInCcrU() {
        val ratingGroup = 10
        val serviceIdentifier = 1
        val session = client!!.createSession(object : Any() {}.javaClass.enclosingMethod.name)
        val initRequest = client!!.createRequest(
                OCS_REALM,
                OCS_HOST,
                session!!
        )
        createInitRequest(initRequest!!.avps, MSISDN, 500000L, ratingGroup, serviceIdentifier)
        client!!.sendNextRequest(initRequest, session)
        waitForAnswer(session.sessionId)
        try {
            val result = client!!.getAnswer(session.sessionId)
            Assert.assertEquals(DIAMETER_SUCCESS, result!!.resultCode!!.toLong())
            val resultAvps = result.resultAvps
            Assert.assertEquals(OCS_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
            Assert.assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
            Assert.assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32)
            val resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
            Assert.assertEquals(DIAMETER_SUCCESS, resultMSCC.grouped.getAvp(Avp.RESULT_CODE).integer32.toLong())
            Assert.assertEquals(serviceIdentifier.toLong(), resultMSCC.grouped.getAvp(Avp.SERVICE_IDENTIFIER_CCA).unsigned32)
            Assert.assertEquals(ratingGroup.toLong(), resultMSCC.grouped.getAvp(Avp.RATING_GROUP).unsigned32)
            val granted = resultMSCC.grouped.getAvp(Avp.GRANTED_SERVICE_UNIT)
            Assert.assertEquals(500000L, granted.grouped.getAvp(Avp.CC_TOTAL_OCTETS).unsigned64)
        } catch (e: AvpDataException) {
            LOG.error("Failed to get Result-Code", e)
        }
        val updateRequest = client!!.createRequest(
                OCS_REALM,
                OCS_HOST,
                session
        )
        createUpdateRequest(updateRequest!!.avps, MSISDN, -1L, 500000L, ratingGroup, serviceIdentifier, ReportingReason.QHT)
        client!!.sendNextRequest(updateRequest, session)
        waitForAnswer(session.sessionId)
        try {
            val result = client!!.getAnswer(session.sessionId)
            Assert.assertEquals(DIAMETER_SUCCESS, result!!.resultCode!!.toLong())
            val resultAvps = result.resultAvps
            Assert.assertEquals(OCS_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
            Assert.assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
            Assert.assertEquals(RequestType.UPDATE_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32)
            val resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
            Assert.assertNull("No requested MSCC", resultMSCC)
            Assert.assertEquals(86400, resultAvps.getAvp(Avp.VALIDITY_TIME).integer32.toLong())
        } catch (e: AvpDataException) {
            LOG.error("Failed to get Result-Code", e)
        }
    }

    @Test
    fun testReAuthRequest() {
        val session = client!!.createSession(object : Any() {}.javaClass.enclosingMethod.name)
        simpleCreditControlRequestInit(session, 500000L, 500000L, 10, 1)
        OcsServer.sendReAuthRequest(SessionContext(session!!.sessionId, PGW_HOST, PGW_REALM, APN, MCC_MNC))
        waitForRequest(session.sessionId)
        try {
            val result = client!!.getRequest(session.sessionId)
            val resultAvps = result!!.resultAvps
            Assert.assertEquals(ReAuthRequestType.AUTHORIZE_ONLY.ordinal.toLong(), resultAvps.getAvp(Avp.RE_AUTH_REQUEST_TYPE).integer32.toLong())
            Assert.assertEquals(PGW_HOST, resultAvps.getAvp(Avp.DESTINATION_HOST).utF8String)
            Assert.assertEquals(PGW_REALM, resultAvps.getAvp(Avp.DESTINATION_REALM).utF8String)
            Assert.assertEquals(OCS_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
            Assert.assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
        } catch (e: AvpDataException) {
            LOG.error("Failed to get Avp", e)
        }
        session.release()
    }

    // Currently not used in testing
    @DisplayName("Service-Information Credit-Control-Request Init")
    @Throws(UnsupportedEncodingException::class)
    fun serviceInformationCreditControlRequestInit() {
        val session = client!!.createSession(object : Any() {}.javaClass.enclosingMethod.name)
        val request = client!!.createRequest(
                OCS_REALM,
                OCS_HOST,
                session!!
        )
        val ccrAvps = request!!.avps
        createInitRequest(ccrAvps, MSISDN, 500000L, 10, 1)
        val serviceInformation = ccrAvps.addGroupedAvp(Avp.SERVICE_INFORMATION, VENDOR_ID_3GPP.toLong(), true, false)
        val psInformation = serviceInformation.addGroupedAvp(Avp.PS_INFORMATION, VENDOR_ID_3GPP.toLong(), true, false)
        psInformation.addAvp(Avp.TGPP_CHARGING_ID, "01aaacf", VENDOR_ID_3GPP.toLong(), true, false, true)
        psInformation.addAvp(Avp.TGPP_PDP_TYPE, 0, VENDOR_ID_3GPP.toLong(), true, false) // IPv4
        try {
            psInformation.addAvp(Avp.PDP_ADDRESS, InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)), VENDOR_ID_3GPP.toLong(), true, false)
            psInformation.addAvp(Avp.SGSN_ADDRESS, InetAddress.getByAddress(byteArrayOf(8, 0, 0, 6)), VENDOR_ID_3GPP.toLong(), true, false)
            psInformation.addAvp(Avp.GGSN_ADDRESS, InetAddress.getByAddress(byteArrayOf(2, 0, 0, 6)), VENDOR_ID_3GPP.toLong(), true, false)
        } catch (e: UnknownHostException) {
            LOG.info("Failed to add address")
        }
        psInformation.addAvp(Avp.TGPP_IMSI_MCC_MNC, "24201", VENDOR_ID_3GPP.toLong(), true, false, false)
        psInformation.addAvp(Avp.TGPP_GGSN_MCC_MNC, "24006", VENDOR_ID_3GPP.toLong(), true, false, false)
        psInformation.addAvp(Avp.TGPP_NSAPI, "6", VENDOR_ID_3GPP.toLong(), true, false, true)
        psInformation.addAvp(30, "loltel", false) // Called-Station-Id
        psInformation.addAvp(Avp.TGPP_SESSION_STOP_INDICATOR, "\u00ff", VENDOR_ID_3GPP.toLong(), true, false, false)
        psInformation.addAvp(Avp.TGPP_SELECTION_MODE, "0", VENDOR_ID_3GPP.toLong(), true, false, false)
        psInformation.addAvp(Avp.TGPP_CHARGING_CHARACTERISTICS, "0800", VENDOR_ID_3GPP.toLong(), true, false, true)
        psInformation.addAvp(Avp.GPP_SGSN_MCC_MNC, "24201", VENDOR_ID_3GPP.toLong(), true, false, false)
        val timeZoneBytes = byteArrayOf(64, 0)
        val timeZone = String(timeZoneBytes, StandardCharsets.UTF_8)
        psInformation.addAvp(Avp.TGPP_MS_TIMEZONE, timeZone, VENDOR_ID_3GPP.toLong(), true, false, true)
        psInformation.addAvp(Avp.CHARGING_RULE_BASE_NAME, "RB1", VENDOR_ID_3GPP.toLong(), true, false, false)
        val ratTypeBytes = byteArrayOf(6)
        val ratType = String(ratTypeBytes, StandardCharsets.UTF_8)
        psInformation.addAvp(Avp.TGPP_RAT_TYPE, ratType, VENDOR_ID_3GPP.toLong(), true, false, true)
        val s = "8242f21078b542f2100103c703"
        psInformation.addAvp(Avp.GPP_USER_LOCATION_INFO, DatatypeConverter.parseHexBinary(s), VENDOR_ID_3GPP.toLong(), true, false)
        client!!.sendNextRequest(request, session)
        waitForAnswer(session.sessionId)
        try {
            val result = client!!.getAnswer(session.sessionId)
            Assert.assertEquals(DIAMETER_SUCCESS, result!!.resultCode!!.toLong())
            val resultAvps = result.resultAvps
            Assert.assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32.toLong())
            val resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
            Assert.assertEquals(4012L, resultMSCC.grouped.getAvp(Avp.RESULT_CODE).integer32.toLong())
            Assert.assertEquals(1, resultMSCC.grouped.getAvp(Avp.SERVICE_IDENTIFIER_CCA).integer32.toLong())
            val granted = resultMSCC.grouped.getAvp(Avp.GRANTED_SERVICE_UNIT)
            Assert.assertEquals(0L, granted.grouped.getAvp(Avp.CC_TOTAL_OCTETS).unsigned64)
        } catch (e: AvpDataException) {
            LOG.error("Failed to get Result-Code", e)
        }
    }

    private fun waitForAnswer(sessionId: String) {
        var i = 0
        while (!client!!.isAnswerReceived(sessionId) && i < 100) {
            i++
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) { // continue
            }
        }
        Assert.assertEquals(true, client!!.isAnswerReceived(sessionId))
    }

    private fun waitForRequest(sessionId: String) {
        var i = 0
        while (!client!!.isRequestReceived(sessionId) && i < 100) {
            i++
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) { // continue
            }
        }
        Assert.assertEquals(true, client!!.isRequestReceived(sessionId))
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OcsApplicationTest::class.java)
        private const val VENDOR_ID_3GPP = 10415
        private const val OCS_REALM = "loltel_ocs"
        private const val OCS_HOST = "ocs"
        private const val PGW_HOST = "pgw"
        private const val PGW_REALM = "loltel_pgw"
        private const val APN = "loltel-test"
        private const val MCC_MNC = "24201"
        private const val MSISDN = "4790300123"
        private const val DIAMETER_SUCCESS = 2001L
        private var client: TestClient? = null
        // The same OcsApplication will be used in all test cases
        private val application = OcsApplication()

        @BeforeAll
        @JvmStatic
        fun setUp() {
            application.start("src/test/resources/", "server-jdiameter-config.xml")
            client = TestClient()
            client!!.initStack("src/test/resources/", "client-jdiameter-config.xml")
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            client!!.shutdown()
            client = null
            OcsApplication.shutdown()
        }
    }
}