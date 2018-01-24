package com.telenordigital.ocsgw;

import com.telenordigital.ocsgw.diameter.FinalUnitAction;
import com.telenordigital.ocsgw.diameter.RequestType;
import com.telenordigital.ocsgw.diameter.SubscriptionType;
import com.telenordigital.ocsgw.testclient.TestClient;
import org.apache.log4j.Logger;
import org.jdiameter.api.*;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.common.impl.app.cca.JCreditControlRequestImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;


/**
 *  Tests for the OcsAppliaction. This will use a TestClient to
 *  actually send Diameter traffic on localhost to the OcsApplication.
 */

@DisplayName("OcsApplicationTest")
class OcsApplicationTest {

    private static final Logger log = Logger.getLogger(OcsApplicationTest.class);

    private final String destRealm = "loltel";
    private final String destHost = "ocs";
    private final int commandCode = 272; // Credit-Control
    private final long applicationID = 4L;  // Diameter Credit Control Application (4)

    private TestClient client;

    // The same OcsApplication will be used in all test cases
    private OcsApplication application = new OcsApplication();

    @BeforeEach
    protected void setUp() {
        client = new TestClient();
        client.initStack();
        client.start();
    }

    @AfterEach
    protected void tearDown() {
        client.shutdown();
        client = null;
    }

    private void simpleCreditControlRequestInit() {

        Request request = client.getSession().createRequest(
                commandCode,
                ApplicationId.createByAuthAppId(applicationID),
                destRealm,
                destHost
        );

        AvpSet ccrAvps = request.getAvps();
        ccrAvps.addAvp(Avp.CC_REQUEST_TYPE, RequestType.INITIAL_REQUEST, true, false);
        ccrAvps.addAvp(Avp.CC_REQUEST_NUMBER, 0, true, false);

        AvpSet subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, "4790300123", false);

        ccrAvps.addAvp(Avp.MULTIPLE_SERVICES_INDICATOR, 1);

        AvpSet mscc = ccrAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
        mscc.addAvp(Avp.RATING_GROUP, 10);
        AvpSet requestedServiceUnits = mscc.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
        requestedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, 500000L);
        requestedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L);
        requestedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L);

        JCreditControlRequest ccr = new JCreditControlRequestImpl(request);

        client.setRequest(ccr);
        client.sendNextRequest();

        while (client.isAnswerReceived()) {
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
                log.error("Start Failed", e);
            }
        }
        try {
            assertEquals(2001L, client.getResultCodeAvp().getInteger32());
            AvpSet resultAvps = client.getResultAvps();
            assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(2001L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(500000L, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            log.error("Failed to get Result-Code", e);
        }
    }

    private void simpleCreditControlRequestUpdate() {

        Request request = client.getSession().createRequest(
                commandCode,
                ApplicationId.createByAuthAppId(applicationID),
                destRealm,
                destHost
        );

        AvpSet ccrAvps = request.getAvps();
        ccrAvps.addAvp(Avp.CC_REQUEST_TYPE, RequestType.UPDATE_REQUEST, true, false);
        ccrAvps.addAvp(Avp.CC_REQUEST_NUMBER, 1, true, false);

        AvpSet subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, "4790300123", false);

        ccrAvps.addAvp(Avp.MULTIPLE_SERVICES_INDICATOR, 1);

        AvpSet mscc = ccrAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
        mscc.addAvp(Avp.RATING_GROUP, 10);
        AvpSet requestedServiceUnits = mscc.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
        requestedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, 400000L);
        requestedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L);
        requestedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L);

        JCreditControlRequest ccr = new JCreditControlRequestImpl(request);

        client.setRequest(ccr);
        client.sendNextRequest();

        while (client.isAnswerReceived()) {
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
                log.error("Start Failed", e);
            }
        }
        try {
            assertEquals(2001L, client.getResultCodeAvp().getInteger32());
            AvpSet resultAvps = client.getResultAvps();
            assertEquals(RequestType.UPDATE_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(2001L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(400000L, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            log.error("Failed to get Result-Code", e);
        }

    }

    @Test
    @DisplayName("Simple Credit-Control-Request Init Update and Terminate")
    public void simpleCreditControlRequestInitUpdateAndTerminate() {
        simpleCreditControlRequestInit();
        simpleCreditControlRequestUpdate();

        Request request = client.getSession().createRequest(
                commandCode,
                ApplicationId.createByAuthAppId(applicationID),
                destRealm,
                destHost
        );

        AvpSet ccrAvps = request.getAvps();
        ccrAvps.addAvp(Avp.CC_REQUEST_TYPE, RequestType.TERMINATION_REQUEST, true, false);
        ccrAvps.addAvp(Avp.CC_REQUEST_NUMBER, 2, true, false);

        AvpSet subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, "4790300123", false);

        ccrAvps.addAvp(Avp.TERMINATION_CAUSE, 1, true, false); // 1 = DIAMETER_LOGOUT

        AvpSet mscc = ccrAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
        mscc.addAvp(Avp.RATING_GROUP, 10);
        AvpSet usedServiceUnits = mscc.addGroupedAvp(Avp.USED_SERVICE_UNIT);
        usedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, 700000L);
        usedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L);
        usedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L);
        usedServiceUnits.addAvp(Avp.CC_SERVICE_SPECIFIC_UNITS, 0L);
        mscc.addAvp(Avp.REPORTING_REASON, 2, 10415, true, false); // 2 = FINAL , 10415 = 3GPP

        JCreditControlRequest ccr = new JCreditControlRequestImpl(request);

        client.setRequest(ccr);
        client.sendNextRequest();

        while (client.isAnswerReceived()) {
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
                log.error("Start Failed", e);
            }
        }
        try {
            assertEquals(2001L, client.getResultCodeAvp().getInteger32());
            AvpSet resultAvps = client.getResultAvps();
            assertEquals(RequestType.TERMINATION_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(2001L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(0L, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
            AvpSet finalUnitIndication = resultMSCC.getGrouped().getAvp(Avp.FINAL_UNIT_INDICATION).getGrouped();
            assertEquals(FinalUnitAction.TERMINATE, finalUnitIndication.getAvp(Avp.FINAL_UNIT_ACTION).getInteger32());
        } catch (AvpDataException e) {
            log.error("Failed to get Result-Code", e);
        }
    }
}