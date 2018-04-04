package org.ostelco.ext_pgw;

import org.apache.log4j.Logger;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ostelco.diameter.model.FinalUnitAction;
import org.ostelco.diameter.model.RequestType;
import org.ostelco.diameter.test.TestClient;
import org.ostelco.diameter.test.TestHelper;

import static org.junit.Assert.assertEquals;


/**
 *  Integration tests for the OcsApplication. This test uses the diameter-test lib to setup a test P-GW to
 *  actually send Diameter traffic on the selected DataSource to the OcsApplication. The
 *  DataSource used is the one in the configuration file for this resources.
 *
 *  ToDo: These test should start with a creation of a user in Prime. Now they use a predefined user in Firebase.
 */


public class OcsIntegrationTest {

    private static final Logger LOG = Logger.getLogger(OcsIntegrationTest.class);

    private static final String DEST_REALM = "loltel";
    private static final String DEST_HOST = "ocs";

    private static final String MSISDN = "4747900184";
    private static final long BUCKET_SIZE = 500L;

    private TestClient client;

    @Before
    public void setUp() {
        client = new TestClient();
        client.initStack("/");
    }

    @After
    public void tearDown() {
        client.shutdown();
        client = null;
    }

    private void simpleCreditControlRequestInit() {

        Request request = client.createRequest(
                DEST_REALM,
                DEST_HOST
        );

        TestHelper.createInitRequest(request.getAvps(), MSISDN, BUCKET_SIZE);

        client.sendNextRequest(request);

        waitForAnswer();

        try {
            assertEquals(2001L, client.getResultCodeAvp().getInteger32());
            AvpSet resultAvps = client.getResultAvps();
            assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(2001L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
            assertEquals(1, resultMSCC.getGrouped().getAvp(Avp.SERVICE_IDENTIFIER_CCA).getUnsigned32());
            assertEquals(10, resultMSCC.getGrouped().getAvp(Avp.RATING_GROUP).getUnsigned32());
            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(BUCKET_SIZE, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
    }

    private void simpleCreditControlRequestUpdate() {

        Request request = client.createRequest(
                DEST_REALM,
                DEST_HOST
        );

        TestHelper.creatUpdateRequest(request.getAvps(), MSISDN, BUCKET_SIZE);

        client.sendNextRequest(request);

        waitForAnswer();

        try {
            assertEquals(2001L, client.getResultCodeAvp().getInteger32());
            AvpSet resultAvps = client.getResultAvps();
            assertEquals(RequestType.UPDATE_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(2001L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(BUCKET_SIZE, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }

    }

    @Test
    public void simpleCreditControlRequestInitUpdateAndTerminate() {
        simpleCreditControlRequestInit();
        simpleCreditControlRequestUpdate();

        Request request = client.createRequest(
                DEST_REALM,
                DEST_HOST
        );

        TestHelper.createTerminateRequest(request.getAvps(), MSISDN, BUCKET_SIZE);

        client.sendNextRequest(request);

        waitForAnswer();

        try {
            assertEquals(2001L, client.getResultCodeAvp().getInteger32());
            AvpSet resultAvps = client.getResultAvps();
            assertEquals(RequestType.TERMINATION_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(2001L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(0L, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
            AvpSet finalUnitIndication = resultMSCC.getGrouped().getAvp(Avp.FINAL_UNIT_INDICATION).getGrouped();
            assertEquals(FinalUnitAction.TERMINATE.ordinal(), finalUnitIndication.getAvp(Avp.FINAL_UNIT_ACTION).getInteger32());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
    }


    @Test
    public void creditControlRequestInitNoCredit() {

        Request request = client.createRequest(
                DEST_REALM,
                DEST_HOST
        );

        TestHelper.createInitRequest(request.getAvps(), "4333333333", 500000L);

        client.sendNextRequest(request);

        waitForAnswer();

        try {
            assertEquals(2001L, client.getResultCodeAvp().getInteger32());
            AvpSet resultAvps = client.getResultAvps();
            assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(4012L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
            assertEquals(1, resultMSCC.getGrouped().getAvp(Avp.SERVICE_IDENTIFIER_CCA).getInteger32());
            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(0L, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
    }

    private void waitForAnswer() {
        int i = 0;
        while (!client.isAnswerReceived() && i<10) {
            i++;
            try {
                Thread.currentThread().sleep(500);
            } catch (InterruptedException e) {
                LOG.error("Start Failed", e);
            }
        }
        assertEquals(true, client.isAnswerReceived());
    }
}
