package com.telenordigital.ocsgw;

import com.telenordigital.ext_pgw.TestClient;
import com.telenordigital.ocsgw.diameter.FinalUnitAction;
import com.telenordigital.ocsgw.diameter.RequestType;
import com.telenordigital.ocsgw.diameter.SubscriptionType;
import org.apache.log4j.Logger;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Request;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.common.impl.app.cca.JCreditControlRequestImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;


/**
 *  Tests for the OcsAppliaction. This will use a TestClient to
 *  actually send Diameter traffic on localhost to the OcsApplication.
 */

@DisplayName("OcsApplicationTest")
class OcsApplicationTest {

    private final int VENDOR_ID_3GPP = 10415;

    private static final Logger log = Logger.getLogger(OcsApplicationTest.class);

    private final String destRealm = "loltel";
    private final String destHost = "ocs";
    private final int commandCode = 272; // Credit-Control
    private final long applicationID = 4L;  // Diameter Credit Control Application (4)

    private TestClient client;

    // The same OcsApplication will be used in all test cases
    private OcsApplication application = new OcsApplication();
    private static boolean applicationStarted = false;

    @BeforeEach
    protected void setUp() {
        if (!applicationStarted) {
            application.start("src/test/resources/");
            applicationStarted = true;
        }
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
        mscc.addAvp(Avp.SERVICE_IDENTIFIER_CCA, 1);
        AvpSet requestedServiceUnits = mscc.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
        requestedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, 500000L);
        requestedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L);
        requestedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L);

        JCreditControlRequest ccr = new JCreditControlRequestImpl(request);

        client.setRequest(ccr);
        client.sendNextRequest();

        waitForAnswer();

        try {
            assertEquals(2001L, client.getResultCodeAvp().getInteger32());
            AvpSet resultAvps = client.getResultAvps();
            assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(2001L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
            assertEquals(1, resultMSCC.getGrouped().getAvp(Avp.SERVICE_IDENTIFIER_CCA).getInteger32());
            assertEquals(10, resultMSCC.getGrouped().getAvp(Avp.RATING_GROUP).getInteger32());
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
        mscc.addAvp(Avp.SERVICE_IDENTIFIER_CCA, 1);
        AvpSet requestedServiceUnits = mscc.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
        requestedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, 400000L);
        requestedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L);
        requestedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L);

        JCreditControlRequest ccr = new JCreditControlRequestImpl(request);

        client.setRequest(ccr);
        client.sendNextRequest();

        waitForAnswer();

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
        mscc.addAvp(Avp.SERVICE_IDENTIFIER_CCA, 1);
        AvpSet usedServiceUnits = mscc.addGroupedAvp(Avp.USED_SERVICE_UNIT);
        usedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, 700000L);
        usedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L);
        usedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L);
        usedServiceUnits.addAvp(Avp.CC_SERVICE_SPECIFIC_UNITS, 0L);
        mscc.addAvp(Avp.REPORTING_REASON, 2, VENDOR_ID_3GPP, true, false); // 2 = FINAL

        JCreditControlRequest ccr = new JCreditControlRequestImpl(request);

        client.setRequest(ccr);
        client.sendNextRequest();

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
            assertEquals(FinalUnitAction.TERMINATE, finalUnitIndication.getAvp(Avp.FINAL_UNIT_ACTION).getInteger32());
        } catch (AvpDataException e) {
            log.error("Failed to get Result-Code", e);
        }
    }

    // Currently not used in testing
    @DisplayName("Service-Information Credit-Control-Request Init")
    public void serviceInformationCreditControlRequestInit() {

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
        mscc.addAvp(Avp.SERVICE_IDENTIFIER_CCA, 1);
        AvpSet requestedServiceUnits = mscc.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
        requestedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, 500000L);
        requestedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L);
        requestedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L);

        AvpSet serviceInformation = ccrAvps.addGroupedAvp(Avp.SERVICE_INFORMATION, VENDOR_ID_3GPP, true, false);
        AvpSet psInformation = serviceInformation.addGroupedAvp(Avp.PS_INFORMATION, VENDOR_ID_3GPP, true, false);
        psInformation.addAvp(Avp.TGPP_CHARGING_ID, "01aaacf" , VENDOR_ID_3GPP, true, false, true);
        psInformation.addAvp(Avp.TGPP_PDP_TYPE, 0, VENDOR_ID_3GPP, true, false); // IPv4
        try {
            psInformation.addAvp(Avp.PDP_ADDRESS, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), VENDOR_ID_3GPP, true,  false);
            psInformation.addAvp(Avp.SGSN_ADDRESS, InetAddress.getByAddress(new byte[]{8, 0, 0, 6}), VENDOR_ID_3GPP, true,  false);
            psInformation.addAvp(Avp.GGSN_ADDRESS, InetAddress.getByAddress(new byte[]{2, 0, 0, 6}), VENDOR_ID_3GPP, true,  false);
        } catch (UnknownHostException e) {
            log.info("Failed to add address");
        }
        psInformation.addAvp(Avp.TGPP_IMSI_MCC_MNC, "24201", VENDOR_ID_3GPP, true,  false, false);
        psInformation.addAvp(Avp.TGPP_GGSN_MCC_MNC, "24006", VENDOR_ID_3GPP, true,  false, false);
        psInformation.addAvp(Avp.TGPP_NSAPI, "6", VENDOR_ID_3GPP, true, false, true);
        psInformation.addAvp(30, "loltel", false); // Called-Station-Id
        psInformation.addAvp(Avp.TGPP_SESSION_STOP_INDICATOR, "\377", VENDOR_ID_3GPP, true, false, false);
        psInformation.addAvp(Avp.TGPP_SELECTION_MODE, "0", VENDOR_ID_3GPP, true, false, false);
        psInformation.addAvp(Avp.TGPP_CHARGING_CHARACTERISTICS, "0800", VENDOR_ID_3GPP, true, false, true);
        psInformation.addAvp(Avp.GPP_SGSN_MCC_MNC, "24201", false);
        psInformation.addAvp(Avp.TGPP_MS_TIMEZONE, "4000", VENDOR_ID_3GPP, true, false, true);
        psInformation.addAvp(Avp.CHARGING_RULE_BASE_NAME, "RB1", VENDOR_ID_3GPP, true, false, false);
        psInformation.addAvp(Avp.TGPP_RAT_TYPE, "6", VENDOR_ID_3GPP, true, false, true);
        psInformation.addAvp(Avp.GPP_USER_LOCATION_INFO, "8242f21078b542f2100103c703",VENDOR_ID_3GPP, true, false, false);

        JCreditControlRequest ccr = new JCreditControlRequestImpl(request);

        client.setRequest(ccr);
        client.sendNextRequest();

        waitForAnswer();

        try {
            assertEquals(2001L, client.getResultCodeAvp().getInteger32());
        } catch (AvpDataException e) {
            log.error("Failed to get Result-Code", e);
        }
    }


    private void waitForAnswer() {
        int i = 0;
        while (!client.isAnswerReceived() && i<10) {
            i++;
            try {
                Thread.currentThread().sleep(500);
            } catch (InterruptedException e) {
                log.error("Start Failed", e);
            }
        }
        assertEquals(true, client.isAnswerReceived());
    }
}