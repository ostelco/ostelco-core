package org.ostelco.ocsgw;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Request;
import org.jdiameter.api.Session;
import org.junit.jupiter.api.*;
import org.ostelco.diameter.model.ReAuthRequestType;
import org.ostelco.diameter.model.RequestType;
import org.ostelco.diameter.model.SessionContext;
import org.ostelco.diameter.test.Result;
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
import static org.junit.Assert.fail;


/**
 *  Tests for the OcsApplication. This will use a TestClient to
 *  actually send Diameter traffic on localhost to the OcsApplication.
 */

@DisplayName("OcsApplicationTest")
public class OcsApplicationTest {

    private static final Logger LOG = LoggerFactory.getLogger(OcsApplicationTest.class);

    private static final int VENDOR_ID_3GPP = 10415;

    private static final String OCS_REALM = "loltel_ocs";
    private static final String OCS_HOST = "ocs";
    private static final String PGW_HOST = "pgw";
    private static final String PGW_REALM = "loltel_pgw";
    private static final String APN = "loltel-test";
    private static final String MCC_MNC = "24201";
    private static final String MSISDN = "4790300123";
    private static final long DIAMETER_SUCCESS = 2001L;

    private static TestClient client;

    // The same OcsApplication will be used in all test cases
    private final static OcsApplication application = new OcsApplication();

    @BeforeAll
    public static void setUp() {
        application.start("src/test/resources/", "server-jdiameter-config.xml");

        client = new TestClient();
        client.initStack("src/test/resources/", "client-jdiameter-config.xml");
    }

    @AfterAll
    public static void tearDown() {
        client.shutdown();
        client = null;

        OcsApplication.shutdown();
    }

    private void simpleCreditControlRequestInit(Session session, Long requestedBucketSize, Long expectedGrantedBucketSize, Integer ratingGroup, Integer serviceIdentifier) {

        Request request = client.createRequest(
                OCS_REALM,
                OCS_HOST,
                session
        );

        TestHelper.createInitRequest(request.getAvps(), MSISDN, requestedBucketSize, ratingGroup, serviceIdentifier);

        client.sendNextRequest(request, session);

        waitForAnswer(session.getSessionId());

        try {
            Result result = client.getAnswer(session.getSessionId());
            assertEquals(DIAMETER_SUCCESS, result.getResultCode().longValue());
            AvpSet resultAvps = result.getResultAvps();
            assertEquals(OCS_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).getUTF8String());
            assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).getUTF8String());
            assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(DIAMETER_SUCCESS, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());

            if (serviceIdentifier > 0) {
                assertEquals(serviceIdentifier.longValue(), resultMSCC.getGrouped().getAvp(Avp.SERVICE_IDENTIFIER_CCA).getUnsigned32());
            }

            if (ratingGroup > 0) {
                assertEquals(ratingGroup.longValue(), resultMSCC.getGrouped().getAvp(Avp.RATING_GROUP).getUnsigned32());
            }

            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(expectedGrantedBucketSize.longValue(), granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
    }

    private void simpleCreditControlRequestUpdate(Session session,
                                                  Long requestedBucketSize,
                                                  Long usedBucketSize,
                                                  Long expectedGrantedBucketSize,
                                                  Integer ratingGroup,
                                                  Integer serviceIdentifier) {

        Request request = client.createRequest(
                OCS_REALM,
                OCS_HOST,
                session
        );

        TestHelper.createUpdateRequest(request.getAvps(), MSISDN, requestedBucketSize, usedBucketSize, ratingGroup, serviceIdentifier);

        client.sendNextRequest(request, session);

        waitForAnswer(session.getSessionId());

        try {
            Result result = client.getAnswer(session.getSessionId());
            assertEquals(DIAMETER_SUCCESS, result.getResultCode().longValue());
            AvpSet resultAvps = result.getResultAvps();
            assertEquals(RequestType.UPDATE_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(DIAMETER_SUCCESS, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());

            if (serviceIdentifier > 0) {
                assertEquals(serviceIdentifier.longValue(), resultMSCC.getGrouped().getAvp(Avp.SERVICE_IDENTIFIER_CCA).getUnsigned32());
            }

            if (ratingGroup > 0) {
                assertEquals(ratingGroup.longValue(), resultMSCC.getGrouped().getAvp(Avp.RATING_GROUP).getUnsigned32());
            }

            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(expectedGrantedBucketSize.longValue(), granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }

    }

    @Test
    @DisplayName("Simple Credit-Control-Request Init Update and Terminate")
    public void simpleCreditControlRequestInitUpdateAndTerminate() {

        final int ratingGroup = 10;
        final int serviceIdentifier = 1;

        Session session = client.createSession(new Object() {}.getClass().getEnclosingMethod().getName());
        simpleCreditControlRequestInit(session, 500_000L, 500_000L,ratingGroup, serviceIdentifier);
        simpleCreditControlRequestUpdate(session, 400_000L, 500_000L, 400_000L, ratingGroup, serviceIdentifier);

        Request request = client.createRequest(
                OCS_REALM,
                OCS_HOST,
                session
        );

        TestHelper.createTerminateRequest(request.getAvps(), MSISDN, 400_000L, ratingGroup, 1);

        client.sendNextRequest(request, session);

        waitForAnswer(session.getSessionId());

        try {
            Result result = client.getAnswer(session.getSessionId());
            assertEquals(DIAMETER_SUCCESS, result.getResultCode().longValue());
            AvpSet resultAvps = result.getResultAvps();
            assertEquals(RequestType.TERMINATION_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
        session.release();
    }


    @Test
    @DisplayName("Simple Credit-Control-Request Init Update no Requested-Service-Units")
    public void simpleCreditControlRequestInitUpdateNoRSU() {

        final int ratingGroup = 10;
        final int serviceIdentifier = 1;

        Session session = client.createSession(new Object() {}.getClass().getEnclosingMethod().getName());
        simpleCreditControlRequestInit(session, 500_000L, 500_000L, ratingGroup, serviceIdentifier);

        Request request = client.createRequest(
                OCS_REALM,
                OCS_HOST,
                session
        );

        // Only report usage, no request for new bucket
        TestHelper.createUpdateRequest(request.getAvps(), MSISDN, -1L, 500_000L, ratingGroup, serviceIdentifier);

        client.sendNextRequest(request, session);

        waitForAnswer(session.getSessionId());

        try {
            Result result = client.getAnswer(session.getSessionId());
            assertEquals(DIAMETER_SUCCESS, result.getResultCode().longValue());
            AvpSet resultAvps = result.getResultAvps();
            assertEquals(RequestType.UPDATE_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            assertEquals(86400L, resultAvps.getAvp(Avp.VALIDITY_TIME).getInteger32());

        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
        session.release();
    }



    @Test
    @DisplayName("Credit-Control-Request Init Update and Terminate No Requested-Service-Unit Set")
    public void CreditControlRequestInitUpdateAndTerminateNoRequestedServiceUnit() {

        final int ratingGroup = 10;
        final int serviceIdentifier = -1;

        Session session = client.createSession(new Object() {}.getClass().getEnclosingMethod().getName());
        simpleCreditControlRequestInit(session, 0L, 40_000_000L, ratingGroup, serviceIdentifier);
        simpleCreditControlRequestUpdate(session, 0L, 40_000_000L, 40_000_000L, ratingGroup, serviceIdentifier);

        Request request = client.createRequest(
                OCS_REALM,
                OCS_HOST,
                session
        );

        TestHelper.createTerminateRequest(request.getAvps(), MSISDN, 40_000_000L, ratingGroup, serviceIdentifier);

        client.sendNextRequest(request, session);

        waitForAnswer(session.getSessionId());

        try {
            Result result = client.getAnswer(session.getSessionId());
            assertEquals(DIAMETER_SUCCESS, result.getResultCode().longValue());
            AvpSet resultAvps = result.getResultAvps();
            assertEquals(RequestType.TERMINATION_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
        session.release();
    }

    @Test
    @DisplayName("Credit-Control-Request Multi Ratinggroups Init")
    public void creditControlRequestMultiRatingGroupsInit() {
        Session session = client.createSession(new Object() {}.getClass().getEnclosingMethod().getName());
        Request request = client.createRequest(
                OCS_REALM,
                OCS_HOST,
                session
        );

        TestHelper.createInitRequestMultiRatingGroups(request.getAvps(), MSISDN, 500_000L);

        client.sendNextRequest(request, session);

        waitForAnswer(session.getSessionId());

        try {
            Result result = client.getAnswer(session.getSessionId());
            assertEquals(DIAMETER_SUCCESS, result.getResultCode().longValue());
            AvpSet resultAvps = result.getResultAvps();
            assertEquals(OCS_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).getUTF8String());
            assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).getUTF8String());
            assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            AvpSet resultMSCCs = resultAvps.getAvps(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(3, resultMSCCs.size());
            for ( int i=0; i < resultMSCCs.size(); i++ ) {
                AvpSet mscc = resultMSCCs.getAvpByIndex(i).getGrouped();
                assertEquals(DIAMETER_SUCCESS, mscc.getAvp(Avp.RESULT_CODE).getInteger32());
                Avp granted = mscc.getAvp(Avp.GRANTED_SERVICE_UNIT);
                assertEquals(500_000L, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
                int serviceIdentifier = (int) mscc.getAvp(Avp.SERVICE_IDENTIFIER_CCA).getUnsigned32();
                switch (serviceIdentifier) {
                    case 1 :
                        assertEquals(10, mscc.getAvp(Avp.RATING_GROUP).getUnsigned32());
                        break;
                    case 2 :
                        assertEquals(12, mscc.getAvp(Avp.RATING_GROUP).getUnsigned32());
                        break;
                    case 4 :
                        assertEquals(14, mscc.getAvp(Avp.RATING_GROUP).getUnsigned32());
                        break;
                    default:
                        fail("Unexpected Service-Identifier");

                }

            }
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
    }

    @Test
    @DisplayName("test AVP not in Diameter dictionary")
    public void testUnknownAVP() {

        final int ratingGroup = 10;
        final int serviceIdentifier = 1;

        Session session = client.createSession(new Object() {}.getClass().getEnclosingMethod().getName());
        Request request = client.createRequest(
                OCS_REALM,
                OCS_HOST,
                session
        );

        TestHelper.createInitRequest(request.getAvps(), MSISDN, 500_000L, ratingGroup, serviceIdentifier);
        TestHelper.addUnknownApv(request.getAvps());

        client.sendNextRequest(request, session);

        waitForAnswer(session.getSessionId());

        try {
            Result result = client.getAnswer(session.getSessionId());
            assertEquals(DIAMETER_SUCCESS, result.getResultCode().longValue());
            AvpSet resultAvps = result.getResultAvps();
            assertEquals(OCS_HOST, resultAvps.getAvp(Avp.ORIGIN_HOST).getUTF8String());
            assertEquals(OCS_REALM, resultAvps.getAvp(Avp.ORIGIN_REALM).getUTF8String());
            assertEquals(RequestType.INITIAL_REQUEST, resultAvps.getAvp(Avp.CC_REQUEST_TYPE).getInteger32());
            Avp resultMSCC = resultAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
            assertEquals(DIAMETER_SUCCESS, resultMSCC.getGrouped().getAvp(Avp.RESULT_CODE).getInteger32());
            assertEquals(serviceIdentifier, resultMSCC.getGrouped().getAvp(Avp.SERVICE_IDENTIFIER_CCA).getUnsigned32());
            assertEquals(ratingGroup, resultMSCC.getGrouped().getAvp(Avp.RATING_GROUP).getUnsigned32());
            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(500_000L, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
    }

    @Test
    public void testReAuthRequest() {
        Session session = client.createSession(new Object() {}.getClass().getEnclosingMethod().getName());
        simpleCreditControlRequestInit(session, 500_000L, 500_000L, 10, 1);

        OcsServer.INSTANCE.sendReAuthRequest(new SessionContext(session.getSessionId(), PGW_HOST, PGW_REALM, APN, MCC_MNC));
        waitForRequest(session.getSessionId());
        try {
            Result result = client.getRequest(session.getSessionId());
            AvpSet resultAvps = result.getResultAvps();
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

        Session session = client.createSession(new Object() {}.getClass().getEnclosingMethod().getName());
        Request request = client.createRequest(
                OCS_REALM,
                OCS_HOST,
                session
        );

        AvpSet ccrAvps = request.getAvps();
        TestHelper.createInitRequest(ccrAvps, MSISDN, 500000L, 10, 1);

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

        waitForAnswer(session.getSessionId());

        try {
            Result result = client.getAnswer(session.getSessionId());
            assertEquals(DIAMETER_SUCCESS, result.getResultCode().longValue());
            AvpSet resultAvps = result.getResultAvps();
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

    private void waitForAnswer(String sessionId) {
        int i = 0;
        while (!client.isAnswerReceived(sessionId) && i<100) {
            i++;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // continue
            }
        }
        assertEquals(true, client.isAnswerReceived(sessionId));
    }

    private void waitForRequest(String sessionId) {
        int i = 0;
        while (!client.isRequestReceived(sessionId) && i<100) {
            i++;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // continue
            }
        }
        assertEquals(true, client.isRequestReceived(sessionId));
    }
}