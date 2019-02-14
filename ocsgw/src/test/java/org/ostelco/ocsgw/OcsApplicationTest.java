package org.ostelco.ocsgw;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Request;
import org.jdiameter.api.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ostelco.diameter.model.ReAuthRequestType;
import org.ostelco.diameter.model.RequestType;
import org.ostelco.diameter.model.SessionContext;
import org.ostelco.diameter.test.TestClient;
import org.ostelco.diameter.test.TestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;


/**
 *  Tests for the OcsApplication. This will use a TestClient to
 *  actually send Diameter traffic on localhost to the OcsApplication.
 */

@DisplayName("OcsApplicationTest")
public class OcsApplicationTest {

    private static final Logger LOG = LoggerFactory.getLogger(OcsApplicationTest.class);

    private static final int VENDOR_ID_3GPP = 10415;

    private static final String OCS_REALM = "loltel";
    private static final String OCS_HOST = "ocs";
    private static final String PGW_HOST = "testclient";
    private static final String PGW_REALM = "loltel";
    private static final String APN = "loltel-test";
    private static final String MCC_MNC = "24201";

    private static final String MSISDN = "4790300123";

    private TestClient client;

    // The same OcsApplication will be used in all test cases
    private final OcsApplication application = new OcsApplication();

    @BeforeEach
    protected void setUp() {
        application.start("src/test/resources/", "server-jdiameter-config.xml");

        client = new TestClient();
        client.initStack("src/test/resources/", "client-jdiameter-config.xml");
    }

    @AfterEach
    protected void tearDown() {
        client.shutdown();
        client = null;

        OcsApplication.shutdown();
    }

    private void simpleCreditControlRequestInit(Session session) {

        Request request = client.createRequest(
                OCS_REALM,
                OCS_HOST,
                session
        );

        TestHelper.createInitRequest(request.getAvps(), MSISDN, 500000L);

        client.sendNextRequest(request, session);

        waitForAnswer();

        try {
            assertEquals(2001L, client.getResultCodeAvp().getInteger32());
            AvpSet resultAvps = client.getResultAvps();
            assertEquals(OCS_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).getUTF8String());
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

        Request request = client.createRequest(
                OCS_REALM,
                OCS_HOST,
                session
        );

        TestHelper.createUpdateRequest(request.getAvps(), MSISDN, 400000L, 500000L);

        client.sendNextRequest(request, session);

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
            LOG.error("Failed to get Result-Code", e);
        }

    }

    @Test
    @DisplayName("Simple Credit-Control-Request Init Update and Terminate")
    public void simpleCreditControlRequestInitUpdateAndTerminate() {
        Session session = client.createSession();
        simpleCreditControlRequestInit(session);
        simpleCreditControlRequestUpdate(session);

        Request request = client.createRequest(
                OCS_REALM,
                OCS_HOST,
                session
        );

        TestHelper.createTerminateRequest(request.getAvps(), MSISDN, 700000L);

        client.sendNextRequest(request, session);

        waitForAnswer();

        try {
            assertEquals(2001L, client.getResultCodeAvp().getInteger32());
            AvpSet resultAvps = client.getResultAvps();
            assertEquals(RequestType.TERMINATION_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(2001L, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
        session.release();
    }

    @Test
    public void testReAuthRequest() {
        Session session = client.createSession();
        simpleCreditControlRequestInit(session);


        client.initRequestTest();
        OcsServer.getInstance().sendReAuthRequest(new SessionContext(session.getSessionId(), PGW_HOST, PGW_REALM, APN, MCC_MNC));
        waitForRequest();
        try {
            AvpSet resultAvps = client.getResultAvps();
            assertEquals(ReAuthRequestType.AUTHORIZE_ONLY.ordinal(), resultAvps.getAvp(Avp.RE_AUTH_REQUEST_TYPE).getInteger32());
            assertEquals(PGW_HOST, resultAvps.getAvp(Avp.DESTINATION_HOST).getUTF8String());
            assertEquals(PGW_REALM, resultAvps.getAvp(Avp.DESTINATION_REALM).getUTF8String());
            assertEquals(OCS_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).getUTF8String());
            assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).getUTF8String());

        } catch (AvpDataException e) {
            LOG.error("Failed to get Avp", e);
        }
        session.release();
    }

    // Currently not used in testing
    @DisplayName("Service-Information Credit-Control-Request Init")
    public void serviceInformationCreditControlRequestInit() throws UnsupportedEncodingException {

        Session session = client.createSession();
        Request request = client.createRequest(
                OCS_REALM,
                OCS_HOST,
                session
        );

        AvpSet ccrAvps = request.getAvps();
        TestHelper.createInitRequest(ccrAvps, MSISDN, 500000L);

        AvpSet serviceInformation = ccrAvps.addGroupedAvp(Avp.SERVICE_INFORMATION, VENDOR_ID_3GPP, true, false);
        AvpSet psInformation = serviceInformation.addGroupedAvp(Avp.PS_INFORMATION, VENDOR_ID_3GPP, true, false);
        psInformation.addAvp(Avp.TGPP_CHARGING_ID, "01aaacf" , VENDOR_ID_3GPP, true, false, true);
        psInformation.addAvp(Avp.TGPP_PDP_TYPE, 0, VENDOR_ID_3GPP, true, false); // IPv4
        try {
            psInformation.addAvp(Avp.PDP_ADDRESS, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), VENDOR_ID_3GPP, true,  false);
            psInformation.addAvp(Avp.SGSN_ADDRESS, InetAddress.getByAddress(new byte[]{8, 0, 0, 6}), VENDOR_ID_3GPP, true,  false);
            psInformation.addAvp(Avp.GGSN_ADDRESS, InetAddress.getByAddress(new byte[]{2, 0, 0, 6}), VENDOR_ID_3GPP, true,  false);
        } catch (UnknownHostException e) {
            LOG.info("Failed to add address");
        }
        psInformation.addAvp(Avp.TGPP_IMSI_MCC_MNC, "24201", VENDOR_ID_3GPP, true,  false, false);
        psInformation.addAvp(Avp.TGPP_GGSN_MCC_MNC, "24006", VENDOR_ID_3GPP, true,  false, false);
        psInformation.addAvp(Avp.TGPP_NSAPI, "6", VENDOR_ID_3GPP, true, false, true);
        psInformation.addAvp(30, "loltel", false); // Called-Station-Id
        psInformation.addAvp(Avp.TGPP_SESSION_STOP_INDICATOR, "\377", VENDOR_ID_3GPP, true, false, false);
        psInformation.addAvp(Avp.TGPP_SELECTION_MODE, "0", VENDOR_ID_3GPP, true, false, false);
        psInformation.addAvp(Avp.TGPP_CHARGING_CHARACTERISTICS, "0800", VENDOR_ID_3GPP, true, false, true);
        psInformation.addAvp(Avp.GPP_SGSN_MCC_MNC, "24201", VENDOR_ID_3GPP, true, false, false);
        byte[] timeZoneBytes = new byte[] {64, 00};
        String timeZone = new String(timeZoneBytes, StandardCharsets.UTF_8);
        psInformation.addAvp(Avp.TGPP_MS_TIMEZONE, timeZone, VENDOR_ID_3GPP, true, false, true);
        psInformation.addAvp(Avp.CHARGING_RULE_BASE_NAME, "RB1", VENDOR_ID_3GPP, true, false, false);
        byte[] ratTypeBytes = new byte[] {06};
        String ratType = new String(ratTypeBytes, StandardCharsets.UTF_8);
        psInformation.addAvp(Avp.TGPP_RAT_TYPE, ratType , VENDOR_ID_3GPP, true, false, true);

        String s = "8242f21078b542f2100103c703";
        psInformation.addAvp(Avp.GPP_USER_LOCATION_INFO, DatatypeConverter.parseHexBinary(s), VENDOR_ID_3GPP, true, false);

        client.sendNextRequest(request, session);

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
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // continue
            }
        }
        assertEquals(true, client.isAnswerReceived());
    }

    private void waitForRequest() {
        int i = 0;
        while (!client.isRequestReceived() && i<10) {
            i++;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // continue
            }
        }
        assertEquals(true, client.isRequestReceived());
    }
}