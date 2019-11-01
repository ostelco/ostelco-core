package org.ostelco.ocsgw;

import org.jdiameter.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ostelco.diameter.model.ReportingReason;
import org.ostelco.diameter.model.RequestType;
import org.ostelco.diameter.test.Result;
import org.ostelco.diameter.test.TestClient;
import org.ostelco.diameter.test.TestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import static org.junit.Assert.assertEquals;


/**
 *  Tests for the OcsApplication. This will use a TestClient to
 *  actually send Diameter traffic on localhost to the OcsApplication.
 */

@DisplayName("OcsHATest")
public class OcsHATest {

    private static final Logger logger = LoggerFactory.getLogger(OcsHATest.class);


    private static final String OCS_REALM = "loltel";
    private static final String OCS_HOST_1 = "ocs_1";
    private static final String OCS_HOST_2 = "ocs_2";

    private static final long DIAMETER_SUCCESS = 2001L;

    private static final String MSISDN = "4790300123";

    private TestClient testPGW;

    private Process ocsgw_1;
    private Process ocsgw_2;


    private void waitForServerToStart(final int server) {
        switch (server) {
            case 1:
                waitForPort("127.0.0.1", 3868,10000);
                break;
            case 2:
                waitForPort("127.0.0.1", 3869,10000);
                break;
            default:
        }
    }

    private void waitForPort(String hostname, int port, long timeoutMs) {
        logger.debug("Waiting for port " + port);
        long startTs = System.currentTimeMillis();
        boolean scanning = true;
        while (scanning)
        {
            if (System.currentTimeMillis() - startTs > timeoutMs) {
                logger.error("Timeout waiting for port " + port);
                scanning = false;
            }
            try
            {
                SocketAddress addr = new InetSocketAddress(hostname, port);
                SocketChannel socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(true);
                try {
                    socketChannel.connect(addr);
                }
                finally {
                    socketChannel.close();
                }

                scanning = false;
            }
            catch(IOException e)
            {
                logger.debug("Still waiting for port " + port);
                try
                {
                    Thread.sleep(2000);//2 seconds
                }
                catch(InterruptedException ie){
                    logger.error("Interrupted", ie);
                }
            }
        }
        logger.debug("Port " + port + " ready.");
    }

    private void waitForProcessExit(Process process) {
        while (process.isAlive()) {
            try
            {
                Thread.sleep(2000);//2 seconds
            }
            catch(InterruptedException ie){
                logger.error("Interrupted", ie);
            }
        }
    }

    private Process startServer(final int server) {

        Process process = null;
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", "./build/libs/ocsgw-uber.jar");
        processBuilder.environment().put("DIAMETER_CONFIG_FILE", "server-jdiameter-ha-" + server +"-config.xml");
        processBuilder.environment().put("OCS_DATASOURCE_TYPE", "Local");
        processBuilder.environment().put("CONFIG_FOLDER", "src/test/resources/");
        processBuilder.inheritIO();
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            logger.error("Failed to start external OCSgw number" + server, e);
        }
        return process;
    }


    @BeforeEach
    protected void setUp() {
        logger.debug("setUp()");

        ocsgw_1 = startServer(1);
        ocsgw_2 = startServer(2);

        waitForServerToStart(1);
        waitForServerToStart(2);

        testPGW = new TestClient();
        testPGW.initStack("src/test/resources/", "client-jdiameter-ha-config.xml");
    }

    @AfterEach
    protected void tearDown() {
        logger.debug("tearDown()");
        testPGW.shutdown();
        testPGW = null;

        ocsgw_1.destroy();
        ocsgw_2.destroy();
    }

    private void haCreditControlRequestInit(Session session, String host) {

        Request request = testPGW.createRequest(
                OCS_REALM,
                host,
                session
        );

        TestHelper.createInitRequest(request.getAvps(), MSISDN, 500000L, 1, 10);

        testPGW.sendNextRequest(request, session);

        waitForAnswer(session.getSessionId());

        try {
            Result result = testPGW.getAnswer(session.getSessionId());
            assertEquals(DIAMETER_SUCCESS, result.getResultCode().longValue());
            AvpSet resultAvps = result.getResultAvps();
            assertEquals(host, resultAvps.getAvp(Avp.ORIGIN_HOST).getUTF8String());
            assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).getUTF8String());
            assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(2001L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
            assertEquals(10, resultMSCC.getGrouped().getAvp(Avp.SERVICE_IDENTIFIER_CCA).getUnsigned32());
            assertEquals(1, resultMSCC.getGrouped().getAvp(Avp.RATING_GROUP).getUnsigned32());
            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(500000L, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            logger.error("Failed to get Result-Code", e);
        }
    }

    private void restartServer(final int server) {
        switch (server) {
            case 1:
                stopServer(1);
                waitForProcessExit(ocsgw_1);
                ocsgw_1 = startServer(1);
                waitForServerToStart(1);
                break;
            case 2:
                stopServer(2);
                waitForProcessExit(ocsgw_2);
                ocsgw_2 = startServer(2);
                waitForServerToStart(2);
                break;
            default:
                logger.info("Incorrect server number : " + server);
        }
        // Give time for ocsgw to reconnect to P-GW
        try
        {
            logger.debug("Pausing testing 10 seconds so that ocsgw can reconnect...");
            Thread.sleep(10000);//10 seconds
            logger.debug("Continue testing");
        }
        catch(InterruptedException ie){
            logger.error("Interrupted", ie);
        }
    }

    private void stopServer(final int server) {
        switch (server) {
            case 1:
                ocsgw_1.destroy();
                break;
            case 2:
                ocsgw_2.destroy();
                break;
            default:
        }
    }

    private void haCreditControlRequestUpdate(Session session, String host) {

        Request request = testPGW.createRequest(
                OCS_REALM,
                host,
                session
        );

        TestHelper.createUpdateRequest(request.getAvps(), MSISDN, 400000L, 500000L, 1, 10, ReportingReason.QUOTA_EXHAUSTED);

        testPGW.sendNextRequest(request, session);

        waitForAnswer(session.getSessionId());

        try {
            Result result = testPGW.getAnswer(session.getSessionId());
            assertEquals(DIAMETER_SUCCESS, result.getResultCode().longValue());
            AvpSet resultAvps = result.getResultAvps();
            assertEquals(host, resultAvps.getAvp(Avp.ORIGIN_HOST).getUTF8String());
            assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).getUTF8String());
            assertEquals(RequestType.UPDATE_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(2001L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(400000L, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            logger.error("Failed to get Result-Code", e);
        }
    }

    /**
     *  This is only meant to be used for local testing. As it is starting external processes for the
     *  OCSgw you will have to take care to clean up.
     *
     *  The test will create a session. It will restart server 1 before it sends the CCR-Update message.
     *  Then restart server 2 and send another CCR-Update message. This will only work if state was shared
     *  as the session would otherwise have been lost by ocsgw.
     */
    @DisplayName("HA Credit-Control-Request Init Update and Terminate")
    //@Test
    public void haCreditControlRequestInitUpdateAndTerminate() {
        Session session = testPGW.createSession(new Object() {}.getClass().getEnclosingMethod().getName());
        haCreditControlRequestInit(session, OCS_HOST_1);

        // Restart server 1 and continue when it is back online
        restartServer(1);

        haCreditControlRequestUpdate(session, OCS_HOST_1);

        // Stop server 1 and hand over request to server 2
        stopServer(1);
        haCreditControlRequestUpdate(session, OCS_HOST_2);

        // Restart server 2 and continue once it is back up
        restartServer(2);

        Request request = testPGW.createRequest(
                OCS_REALM,
                OCS_HOST_2,
                session
        );

        TestHelper.createTerminateRequest(request.getAvps(), MSISDN, 700000L, 1, 10);

        testPGW.sendNextRequest(request, session);

        waitForAnswer(session.getSessionId());

        try {
            Result result = testPGW.getAnswer(session.getSessionId());
            assertEquals(DIAMETER_SUCCESS, result.getResultCode().longValue());
            AvpSet resultAvps = result.getResultAvps();
            assertEquals(OCS_HOST_2, resultAvps.getAvp(Avp.ORIGIN_HOST).getUTF8String());
            assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).getUTF8String());
            assertEquals(RequestType.TERMINATION_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
        } catch (AvpDataException e) {
            logger.error("Failed to get Result-Code", e);
        }
        session.release();
    }

    private void waitForAnswer(String sessionId) {
        int i = 0;
        while (!testPGW.isAnswerReceived(sessionId) && i<10) {
            i++;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // continue
            }
        }
        assertEquals(true, testPGW.isAnswerReceived(sessionId));
    }
}