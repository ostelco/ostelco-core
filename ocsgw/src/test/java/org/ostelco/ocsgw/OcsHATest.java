package org.ostelco.ocsgw;

import org.jdiameter.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ostelco.diameter.model.RequestType;
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

    private static final Logger LOG = LoggerFactory.getLogger(OcsHATest.class);


    private static final String OCS_REALM = "loltel";
    private static final String OCS_HOST_1 = "ocs_1";
    private static final String OCS_HOST_2 = "ocs_2";

    private static final String MSISDN = "4790300123";

    private TestClient testPGW;

    Process ocsgw_1;
    Process ocsgw_2;


    public void waitForPort(String hostname, int port, long timeoutMs) {
        LOG.info("Waiting for port " + port);
        long startTs = System.currentTimeMillis();
        boolean scanning=true;
        while(scanning)
        {
            if (System.currentTimeMillis() - startTs > timeoutMs) {
                throw new RuntimeException("Timeout waiting for port " + port);
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

                scanning=false;
            }
            catch(IOException e)
            {
                LOG.debug("Still waiting for port " + port);
                try
                {
                    Thread.sleep(2000);//2 seconds
                }
                catch(InterruptedException ie){
                    LOG.error("Interrupted", ie);
                }
            }
        }
        LOG.info("Port " + port + " ready.");
    }

    private void waitForProcessExit(Process process) {
        while (process.isAlive()) {
            try
            {
                Thread.sleep(2000);//2 seconds
            }
            catch(InterruptedException ie){
                LOG.error("Interrupted", ie);
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
            LOG.error("Failed to start external OCSgw number" + server, e);
        }
        return process;
    }


    @BeforeEach
    protected void setUp() {

        ocsgw_1 = startServer(1);
        ocsgw_2 = startServer(2);

        // Wait for ocsgws to start
        waitForPort("127.0.0.1", 3868,10000);
        waitForPort("127.0.0.1", 3869,10000);

        testPGW = new TestClient();
        testPGW.initStack("src/test/resources/", "client-jdiameter-ha-config.xml");
    }

    @AfterEach
    protected void tearDown() {
        testPGW.shutdown();
        testPGW = null;

        ocsgw_1.destroy();
        ocsgw_2.destroy();
    }

    private void haCreditControlRequestInit(Session session) {

        Request request = testPGW.createRequest(
                OCS_REALM,
                OCS_HOST_1,
                session
        );

        TestHelper.createInitRequest(request.getAvps(), MSISDN, 500000L);

        testPGW.sendNextRequest(request, session);

        waitForAnswer();

        try {
            assertEquals(2001L, testPGW.getResultCodeAvp().getInteger32());
            AvpSet resultAvps = testPGW.getResultAvps();
            assertEquals(OCS_HOST_1, resultAvps.getAvp(Avp.ORIGIN_HOST).getUTF8String());
            assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).getUTF8String());
            assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(2001L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
            assertEquals(1, resultMSCC.getGrouped().getAvp(Avp.SERVICE_IDENTIFIER_CCA).getUnsigned32());
            assertEquals(10, resultMSCC.getGrouped().getAvp(Avp.RATING_GROUP).getUnsigned32());
            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(500000L, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
    }

    private void restartServer(final int server) {
        switch (server) {
            case 1:
                ocsgw_1.destroy();
                waitForProcessExit(ocsgw_1);
                ocsgw_1 = startServer(1);
                waitForPort("127.0.0.1", 3868,10000);
                break;
            case 2:
                ocsgw_2.destroy();
                waitForProcessExit(ocsgw_2);
                ocsgw_2 = startServer(2);
                waitForPort("127.0.0.1", 3869,10000);
                break;
            default:
                LOG.info("Incorrect server number : " + server);
        }
        // Give time for ocsgw to reconnect to p-gw
        try
        {
            LOG.info("Pausing testing 10 seconds for ocsgw to reconnect...");
            Thread.sleep(10000);//10 seconds
            LOG.info("Continue testing");
        }
        catch(InterruptedException ie){
            LOG.error("Interrupted", ie);
        }
    }

    private void haCreditControlRequestUpdate(Session session) {

        Request request = testPGW.createRequest(
                OCS_REALM,
                OCS_HOST_1,
                session
        );

        TestHelper.createUpdateRequest(request.getAvps(), MSISDN, 400000L, 500000L);

        testPGW.sendNextRequest(request, session);

        waitForAnswer();

        try {
            assertEquals(2001L, testPGW.getResultCodeAvp().getInteger32());
            AvpSet resultAvps = testPGW.getResultAvps();
            assertEquals(RequestType.UPDATE_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(2001L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(400000L, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
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
    public void haCreditControlRequestInitUpdateAndTerminate() {
        Session session = testPGW.createSession();
        haCreditControlRequestInit(session);
        restartServer(1);
        haCreditControlRequestUpdate(session);
        restartServer(2);
        haCreditControlRequestUpdate(session);

        Request request = testPGW.createRequest(
                OCS_REALM,
                OCS_HOST_1,
                session
        );

        TestHelper.createTerminateRequest(request.getAvps(), MSISDN, 700000L);

        testPGW.sendNextRequest(request, session);

        waitForAnswer();

        try {
            assertEquals(2001L, testPGW.getResultCodeAvp().getInteger32());
            AvpSet resultAvps = testPGW.getResultAvps();
            assertEquals(RequestType.TERMINATION_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(2001L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
        session.release();
    }

    private void waitForAnswer() {
        int i = 0;
        while (!testPGW.isAnswerReceived() && i<10) {
            i++;
            try {
                Thread.currentThread().sleep(500);
            } catch (InterruptedException e) {
                // continue
            }
        }
        assertEquals(true, testPGW.isAnswerReceived());
    }
}