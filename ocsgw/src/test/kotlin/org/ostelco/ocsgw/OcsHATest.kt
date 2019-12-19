package org.ostelco.ocsgw

import org.jdiameter.api.Avp
import org.jdiameter.api.AvpDataException
import org.jdiameter.api.Session
import org.junit.Assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.ostelco.diameter.model.ReportingReason
import org.ostelco.diameter.model.RequestType
import org.ostelco.diameter.test.TestClient
import org.ostelco.diameter.test.TestHelper.createInitRequest
import org.ostelco.diameter.test.TestHelper.createTerminateRequest
import org.ostelco.diameter.test.TestHelper.createUpdateRequest
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.channels.SocketChannel

/**
 * Tests for the OcsApplication. This will use a TestClient to
 * actually send Diameter traffic on localhost to the OcsApplication.
 */
@DisplayName("OcsHATest")
class OcsHATest {
    private var testPGW: TestClient? = null
    private var ocsgw_1: Process? = null
    private var ocsgw_2: Process? = null
    private fun waitForServerToStart(server: Int) {
        when (server) {
            1 -> waitForPort("127.0.0.1", 3868, 10000)
            2 -> waitForPort("127.0.0.1", 3869, 10000)
            else -> {
            }
        }
    }

    private fun waitForPort(hostname: String, port: Int, timeoutMs: Long) {
        logger.debug("Waiting for port $port")
        val startTs = System.currentTimeMillis()
        var scanning = true
        while (scanning) {
            if (System.currentTimeMillis() - startTs > timeoutMs) {
                logger.error("Timeout waiting for port $port")
                scanning = false
            }
            try {
                val addr: SocketAddress = InetSocketAddress(hostname, port)
                val socketChannel = SocketChannel.open()
                socketChannel.configureBlocking(true)
                try {
                    socketChannel.connect(addr)
                } finally {
                    socketChannel.close()
                }
                scanning = false
            } catch (e: IOException) {
                logger.debug("Still waiting for port $port")
                try {
                    Thread.sleep(2000) //2 seconds
                } catch (ie: InterruptedException) {
                    logger.error("Interrupted", ie)
                }
            }
        }
        logger.debug("Port $port ready.")
    }

    private fun waitForProcessExit(process: Process?) {
        while (process!!.isAlive) {
            try {
                Thread.sleep(2000) //2 seconds
            } catch (ie: InterruptedException) {
                logger.error("Interrupted", ie)
            }
        }
    }

    private fun startServer(server: Int): Process? {
        var process: Process? = null
        val processBuilder = ProcessBuilder("java", "-jar", "./build/libs/ocsgw-uber.jar")
        processBuilder.environment()["DIAMETER_CONFIG_FILE"] = "server-jdiameter-ha-$server-config.xml"
        processBuilder.environment()["OCS_DATASOURCE_TYPE"] = "Local"
        processBuilder.environment()["CONFIG_FOLDER"] = "src/test/resources/"
        processBuilder.inheritIO()
        try {
            process = processBuilder.start()
        } catch (e: IOException) {
            logger.error("Failed to start external OCSgw number$server", e)
        }
        return process
    }

    @BeforeEach
    protected fun setUp() {
        logger.debug("setUp()")
        ocsgw_1 = startServer(1)
        ocsgw_2 = startServer(2)
        waitForServerToStart(1)
        waitForServerToStart(2)
        testPGW = TestClient()
        testPGW!!.initStack("src/test/resources/", "client-jdiameter-ha-config.xml")
    }

    @AfterEach
    protected fun tearDown() {
        logger.debug("tearDown()")
        testPGW!!.shutdown()
        testPGW = null
        ocsgw_1!!.destroy()
        ocsgw_2!!.destroy()
    }

    private fun haCreditControlRequestInit(session: Session?, host: String) {
        val request = testPGW!!.createRequest(
                OCS_REALM,
                host,
                session!!
        )
        createInitRequest(request!!.avps, MSISDN, 500000L, 1, 10)
        testPGW!!.sendNextRequest(request, session)
        waitForAnswer(session.sessionId)
        try {
            val result = testPGW!!.getAnswer(session.sessionId)
            Assert.assertEquals(DIAMETER_SUCCESS, result!!.resultCode!!.toLong())
            val resultAvps = result.resultAvps
            Assert.assertEquals(host, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
            Assert.assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
            Assert.assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32.toLong())
            val resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
            Assert.assertEquals(2001L, resultMSCC.grouped.getAvp(Avp.RESULT_CODE).integer32.toLong())
            Assert.assertEquals(10, resultMSCC.grouped.getAvp(Avp.SERVICE_IDENTIFIER_CCA).unsigned32)
            Assert.assertEquals(1, resultMSCC.grouped.getAvp(Avp.RATING_GROUP).unsigned32)
            val granted = resultMSCC.grouped.getAvp(Avp.GRANTED_SERVICE_UNIT)
            Assert.assertEquals(500000L, granted.grouped.getAvp(Avp.CC_TOTAL_OCTETS).unsigned64)
        } catch (e: AvpDataException) {
            logger.error("Failed to get Result-Code", e)
        }
    }

    private fun restartServer(server: Int) {
        when (server) {
            1 -> {
                stopServer(1)
                waitForProcessExit(ocsgw_1)
                ocsgw_1 = startServer(1)
                waitForServerToStart(1)
            }
            2 -> {
                stopServer(2)
                waitForProcessExit(ocsgw_2)
                ocsgw_2 = startServer(2)
                waitForServerToStart(2)
            }
            else -> logger.info("Incorrect server number : $server")
        }
        // Give time for ocsgw to reconnect to P-GW
        try {
            logger.debug("Pausing testing 10 seconds so that ocsgw can reconnect...")
            Thread.sleep(10000) //10 seconds
            logger.debug("Continue testing")
        } catch (ie: InterruptedException) {
            logger.error("Interrupted", ie)
        }
    }

    private fun stopServer(server: Int) {
        when (server) {
            1 -> ocsgw_1!!.destroy()
            2 -> ocsgw_2!!.destroy()
            else -> {
            }
        }
    }

    private fun haCreditControlRequestUpdate(session: Session?, host: String) {
        val request = testPGW!!.createRequest(
                OCS_REALM,
                host,
                session!!
        )
        createUpdateRequest(request!!.avps, MSISDN, 400000L, 500000L, 1, 10, ReportingReason.QUOTA_EXHAUSTED)
        testPGW!!.sendNextRequest(request, session)
        waitForAnswer(session.sessionId)
        try {
            val result = testPGW!!.getAnswer(session.sessionId)
            Assert.assertEquals(DIAMETER_SUCCESS, result!!.resultCode!!.toLong())
            val resultAvps = result.resultAvps
            Assert.assertEquals(host, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
            Assert.assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
            Assert.assertEquals(RequestType.UPDATE_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32.toLong())
            val resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL)
            Assert.assertEquals(2001L, resultMSCC.grouped.getAvp(Avp.RESULT_CODE).integer32.toLong())
            val granted = resultMSCC.grouped.getAvp(Avp.GRANTED_SERVICE_UNIT)
            Assert.assertEquals(400000L, granted.grouped.getAvp(Avp.CC_TOTAL_OCTETS).unsigned64)
        } catch (e: AvpDataException) {
            logger.error("Failed to get Result-Code", e)
        }
    }

    /**
     * This is only meant to be used for local testing. As it is starting external processes for the
     * OCSgw you will have to take care to clean up.
     *
     * The test will create a session. It will restart server 1 before it sends the CCR-Update message.
     * Then restart server 2 and send another CCR-Update message. This will only work if state was shared
     * as the session would otherwise have been lost by ocsgw.
     */
    @DisplayName("HA Credit-Control-Request Init Update and Terminate") //@Test
    fun haCreditControlRequestInitUpdateAndTerminate() {
        val session = testPGW!!.createSession(object : Any() {}.javaClass.enclosingMethod.name)
        haCreditControlRequestInit(session, OCS_HOST_1)
        // Restart server 1 and continue when it is back online
        restartServer(1)
        haCreditControlRequestUpdate(session, OCS_HOST_1)
        // Stop server 1 and hand over request to server 2
        stopServer(1)
        haCreditControlRequestUpdate(session, OCS_HOST_2)
        // Restart server 2 and continue once it is back up
        restartServer(2)
        val request = testPGW!!.createRequest(
                OCS_REALM,
                OCS_HOST_2,
                session!!
        )
        createTerminateRequest(request!!.avps, MSISDN, 700000L, 1, 10)
        testPGW!!.sendNextRequest(request, session)
        waitForAnswer(session.sessionId)
        try {
            val result = testPGW!!.getAnswer(session.sessionId)
            Assert.assertEquals(DIAMETER_SUCCESS, result!!.resultCode!!.toLong())
            val resultAvps = result.resultAvps
            Assert.assertEquals(OCS_HOST_2, resultAvps.getAvp(Avp.ORIGIN_HOST).utF8String)
            Assert.assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).utF8String)
            Assert.assertEquals(RequestType.TERMINATION_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).integer32.toLong())
        } catch (e: AvpDataException) {
            logger.error("Failed to get Result-Code", e)
        }
        session.release()
    }

    private fun waitForAnswer(sessionId: String) {
        var i = 0
        while (!testPGW!!.isAnswerReceived(sessionId) && i < 10) {
            i++
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) { // continue
            }
        }
        Assert.assertEquals(true, testPGW!!.isAnswerReceived(sessionId))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OcsHATest::class.java)
        private const val OCS_REALM = "loltel"
        private const val OCS_HOST_1 = "ocs_1"
        private const val OCS_HOST_2 = "ocs_2"
        private const val DIAMETER_SUCCESS = 2001L
        private const val MSISDN = "4790300123"
    }
}