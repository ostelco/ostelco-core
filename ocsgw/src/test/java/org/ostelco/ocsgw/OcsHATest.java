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

    @BeforeEach
    protected void setUp() {

        {
            ProcessBuilder processBuilderOcsgw1 = new ProcessBuilder("java", "-jar", "./build/libs/ocsgw-uber.jar");
            processBuilderOcsgw1.environment().put("DIAMETER_CONFIG_FILE", "server-jdiameter-ha-1-config.xml");
            processBuilderOcsgw1.environment().put("OCS_DATASOURCE_TYPE", "Local");
            processBuilderOcsgw1.environment().put("CONFIG_FOLDER", "src/test/resources/");
            processBuilderOcsgw1.inheritIO();
            try {
                ocsgw_1 = processBuilderOcsgw1.start();
            } catch (IOException e) {
                LOG.error("Failed to start external OCSgw-1", e);
            }
        }

        {
            ProcessBuilder processBuilerOcsgw2 = new ProcessBuilder("java", "-jar", "./build/libs/ocsgw-uber.jar");
            processBuilerOcsgw2.environment().put("DIAMETER_CONFIG_FILE", "server-jdiameter-ha-2-config.xml");
            processBuilerOcsgw2.environment().put("OCS_DATASOURCE_TYPE", "Local");
            processBuilerOcsgw2.environment().put("CONFIG_FOLDER", "src/test/resources/");
            processBuilerOcsgw2.inheritIO();
            try {
                ocsgw_2 = processBuilerOcsgw2.start();
            } catch (IOException e) {
                LOG.error("Failed to start external OCSgw-2", e);
            }
        }

        // Wait for ocsgws to start
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            LOG.error("I can not get no sleep");
        }

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

    private void simpleCreditControlRequestInit(Session session) {

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

    private void simpleCreditControlRequestUpdate(Session session) {

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

    // Currently not used in testing 
    @DisplayName("HA Credit-Control-Request Init Update and Terminate")
    public void simpleCreditControlRequestInitUpdateAndTerminate() {
        Session session = testPGW.createSession();
        simpleCreditControlRequestInit(session);
        simpleCreditControlRequestUpdate(session);

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