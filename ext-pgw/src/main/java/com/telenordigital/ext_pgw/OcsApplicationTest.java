package com.telenordigital.ext_pgw;

import com.telenordigital.ostelco.diameter.model.FinalUnitAction;
import com.telenordigital.ostelco.diameter.model.RequestType;
import com.telenordigital.ostelco.diameter.model.SubscriptionType;
import org.apache.log4j.Logger;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Request;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.common.impl.app.cca.JCreditControlRequestImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 *  Tests for the OcsAppliaction. This will use a TestClient to
 *  actually send Diameter traffic on localhost to the OcsApplication.
 */

public class OcsApplicationTest {

    private static final Logger LOG = Logger.getLogger(OcsApplicationTest.class);

    private final int VENDOR_ID_3GPP = 10415;

    private static final String DEST_REALM = "loltel";
    private static final String DEST_HOST = "ocs";
    private static final int COMMAND_CODE = 272; // Credit-Control
    private static final long APPLICATION_ID = 4L;  // Diameter Credit Control Application (4)

    private static final String MSISDN = "4747900184";
    private static final String IMSI = "242017100000228";
    private static final String APN = "panacea";
    private static final String SGSN_MCC_MNC = "24201";
    private static final int CALLED_STATION_ID = 30;
    private static final long BUCKET_SIZE = 500L;

    private TestClient client;

    @Before
    public void setUp() {
        client = new TestClient();
        client.initStack();
        client.start();
    }

    @After
    public void tearDown() {
        client.shutdown();
        client = null;
    }

    private void simpleCreditControlRequestInit() {

        Request request = client.getSession().createRequest(
                COMMAND_CODE,
                ApplicationId.createByAuthAppId(APPLICATION_ID),
                DEST_REALM,
                DEST_HOST
        );

        AvpSet ccrAvps = request.getAvps();
        ccrAvps.addAvp(Avp.CC_REQUEST_TYPE, RequestType.INITIAL_REQUEST, true, false);
        ccrAvps.addAvp(Avp.CC_REQUEST_NUMBER, 0, true, false);

        AvpSet subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164.ordinal());
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, MSISDN, false);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_IMSI.ordinal());
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, IMSI, false);

        ccrAvps.addAvp(Avp.MULTIPLE_SERVICES_INDICATOR, 1);

        AvpSet mscc = ccrAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
        mscc.addAvp(Avp.RATING_GROUP, 10);
        mscc.addAvp(Avp.SERVICE_IDENTIFIER_CCA, 1);
        AvpSet requestedServiceUnits = mscc.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
        requestedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, BUCKET_SIZE);
        requestedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L);
        requestedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L);

        AvpSet serviceInformation = ccrAvps.addGroupedAvp(Avp.SERVICE_INFORMATION, VENDOR_ID_3GPP, true, false);
        AvpSet psInformation = serviceInformation.addGroupedAvp(Avp.PS_INFORMATION, VENDOR_ID_3GPP, true, false);
        psInformation.addAvp(CALLED_STATION_ID, APN, false);
        psInformation.addAvp(Avp.GPP_SGSN_MCC_MNC, SGSN_MCC_MNC, VENDOR_ID_3GPP, true, false, true);

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
            Avp granted = resultMSCC.getGrouped().getAvp(Avp.GRANTED_SERVICE_UNIT);
            assertEquals(BUCKET_SIZE, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
    }

    private void simpleCreditControlRequestUpdate() {

        Request request = client.getSession().createRequest(
                COMMAND_CODE,
                ApplicationId.createByAuthAppId(APPLICATION_ID),
                DEST_REALM,
                DEST_HOST
        );

        AvpSet ccrAvps = request.getAvps();
        ccrAvps.addAvp(Avp.CC_REQUEST_TYPE, RequestType.UPDATE_REQUEST, true, false);
        ccrAvps.addAvp(Avp.CC_REQUEST_NUMBER, 1, true, false);

        AvpSet subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164.ordinal());
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, MSISDN, false);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_IMSI.ordinal());
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, IMSI, false);

        ccrAvps.addAvp(Avp.MULTIPLE_SERVICES_INDICATOR, 1);

        AvpSet mscc = ccrAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
        mscc.addAvp(Avp.RATING_GROUP, 10);
        mscc.addAvp(Avp.SERVICE_IDENTIFIER_CCA, 1);
        AvpSet requestedServiceUnits = mscc.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
        requestedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, BUCKET_SIZE);
        requestedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L);
        requestedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L);

        AvpSet serviceInformation = ccrAvps.addGroupedAvp(Avp.SERVICE_INFORMATION, VENDOR_ID_3GPP, true, false);
        AvpSet psInformation = serviceInformation.addGroupedAvp(Avp.PS_INFORMATION, VENDOR_ID_3GPP, true, false);
        psInformation.addAvp(CALLED_STATION_ID, APN, false);
        psInformation.addAvp(Avp.GPP_SGSN_MCC_MNC, SGSN_MCC_MNC, VENDOR_ID_3GPP, true, false, true);

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
            assertEquals(BUCKET_SIZE, granted.getGrouped().getAvp(Avp.CC_TOTAL_OCTETS).getUnsigned64());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }

    }

    @Test
    public void simpleCreditControlRequestInitUpdateAndTerminate() {
        simpleCreditControlRequestInit();
        simpleCreditControlRequestUpdate();

        Request request = client.getSession().createRequest(
                COMMAND_CODE,
                ApplicationId.createByAuthAppId(APPLICATION_ID),
                DEST_REALM,
                DEST_HOST
        );

        AvpSet ccrAvps = request.getAvps();
        ccrAvps.addAvp(Avp.CC_REQUEST_TYPE, RequestType.TERMINATION_REQUEST, true, false);
        ccrAvps.addAvp(Avp.CC_REQUEST_NUMBER, 2, true, false);

        AvpSet subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164.ordinal());
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, MSISDN, false);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_IMSI.ordinal());
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, IMSI, false);

        ccrAvps.addAvp(Avp.TERMINATION_CAUSE, 1, true, false); // 1 = DIAMETER_LOGOUT

        AvpSet mscc = ccrAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
        mscc.addAvp(Avp.RATING_GROUP, 10);
        mscc.addAvp(Avp.SERVICE_IDENTIFIER_CCA, 1);
        AvpSet usedServiceUnits = mscc.addGroupedAvp(Avp.USED_SERVICE_UNIT);
        usedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, BUCKET_SIZE);
        usedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L);
        usedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L);
        usedServiceUnits.addAvp(Avp.CC_SERVICE_SPECIFIC_UNITS, 0L);
        mscc.addAvp(Avp.REPORTING_REASON, 2, 10415, true, false); // 2 = FINAL , 10415 = 3GPP

        AvpSet serviceInformation = ccrAvps.addGroupedAvp(Avp.SERVICE_INFORMATION, VENDOR_ID_3GPP, true, false);
        AvpSet psInformation = serviceInformation.addGroupedAvp(Avp.PS_INFORMATION, VENDOR_ID_3GPP, true, false);
        psInformation.addAvp(CALLED_STATION_ID, APN, false);
        psInformation.addAvp(Avp.GPP_SGSN_MCC_MNC, SGSN_MCC_MNC, VENDOR_ID_3GPP, true, false, true);

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
            assertEquals(FinalUnitAction.TERMINATE.ordinal(), finalUnitIndication.getAvp(Avp.FINAL_UNIT_ACTION).getInteger32());
        } catch (AvpDataException e) {
            LOG.error("Failed to get Result-Code", e);
        }
    }


    @Test
    public void creditControlRequestInitNoCredit() {

        Request request = client.getSession().createRequest(
                COMMAND_CODE,
                ApplicationId.createByAuthAppId(APPLICATION_ID),
                DEST_REALM,
                DEST_HOST
        );

        AvpSet ccrAvps = request.getAvps();
        ccrAvps.addAvp(Avp.CC_REQUEST_TYPE, RequestType.INITIAL_REQUEST, true, false);
        ccrAvps.addAvp(Avp.CC_REQUEST_NUMBER, 0, true, false);

        AvpSet subscriptionId = ccrAvps.addGroupedAvp(Avp.SUBSCRIPTION_ID);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_E164.ordinal());
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, "4333333333", false);
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_TYPE, SubscriptionType.END_USER_IMSI.ordinal());
        subscriptionId.addAvp(Avp.SUBSCRIPTION_ID_DATA, IMSI, false);

        ccrAvps.addAvp(Avp.MULTIPLE_SERVICES_INDICATOR, 1);

        AvpSet mscc = ccrAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
        mscc.addAvp(Avp.RATING_GROUP, 10);
        mscc.addAvp(Avp.SERVICE_IDENTIFIER_CCA, 1);
        AvpSet requestedServiceUnits = mscc.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
        requestedServiceUnits.addAvp(Avp.CC_TOTAL_OCTETS, BUCKET_SIZE);
        requestedServiceUnits.addAvp(Avp.CC_INPUT_OCTETS, 0L);
        requestedServiceUnits.addAvp(Avp.CC_OUTPUT_OCTETS, 0L);

        AvpSet serviceInformation = ccrAvps.addGroupedAvp(Avp.SERVICE_INFORMATION, VENDOR_ID_3GPP, true, false);
        AvpSet psInformation = serviceInformation.addGroupedAvp(Avp.PS_INFORMATION, VENDOR_ID_3GPP, true, false);
        psInformation.addAvp(CALLED_STATION_ID, APN, false);
        psInformation.addAvp(Avp.GPP_SGSN_MCC_MNC, SGSN_MCC_MNC, VENDOR_ID_3GPP, true, false, true);

        JCreditControlRequest ccr = new JCreditControlRequestImpl(request);

        client.setRequest(ccr);
        client.sendNextRequest();

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