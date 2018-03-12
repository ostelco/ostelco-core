package com.telenordigital.ocsgw.diameter;

import com.telenordigital.ocsgw.utils.DiameterUtilities;
import org.jdiameter.api.*;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.common.impl.app.cca.JCreditControlAnswerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Optional;

public class CreditControlContext {

    private static final Logger LOG = LoggerFactory.getLogger(CreditControlContext.class);

    private final ServerCCASession session;
    private final JCreditControlRequest request;
    private final CreditControlRequest ccr;

    private String originHost;
    private String originRealm;

    public CreditControlContext(ServerCCASession session, JCreditControlRequest request) {
        this.session = session;
        this.request = request;
        this.ccr = new CreditControlRequest(request);
    }

    public ServerCCASession getSession() {
        return session;
    }

    public JCreditControlRequest getOriginalCreditControlRequest() {
        return request;
    }

    public CreditControlRequest getCreditControlRequest() { return ccr; }

    public void setOriginHost(String fqdn) { originHost = fqdn; }

    public void setOriginRealm(String realmName) { originRealm = realmName; }

    public void sendCreditControlAnswer(CreditControlAnswer creditControlAnswer) {
        JCreditControlAnswerImpl cca = createCCA(creditControlAnswer);
        if (cca != null) {
            try {
                session.sendCreditControlAnswer(cca);
            } catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException e) {
                LOG.error("Failed to send Credit-Control-Answer", e);
            }
        }
    }

    private JCreditControlAnswerImpl createCCA(CreditControlAnswer creditControlAnswer) {

        JCreditControlAnswerImpl answer = null;
        int resultCode = ResultCode.SUCCESS;

        try {
            answer = new JCreditControlAnswerImpl((Request) request.getMessage(), ResultCode.SUCCESS);

            AvpSet ccaAvps = answer.getMessage().getAvps();

            ccaAvps.addAvp(ccr.getCcRequestType());
            ccaAvps.addAvp(ccr.getCcRequestNumber());

            ccaAvps.addAvp(Avp.ORIGIN_HOST, originHost, true, false, true);
            ccaAvps.addAvp(Avp.ORIGIN_REALM, originRealm, true, false, true);

            final LinkedList<MultipleServiceCreditControl> multipleServiceCreditControls = creditControlAnswer.getMultipleServiceCreditControls();

            for (MultipleServiceCreditControl mscc : multipleServiceCreditControls) {

                AvpSet answerMSCC = ccaAvps.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL, true, false);
                if (mscc.getRatingGroup() > 0) {
                    answerMSCC.addAvp(Avp.RATING_GROUP, mscc.getRatingGroup(), true, false , true);
                }
                if (mscc.getServiceIdentifier() > 0) {
                    answerMSCC.addAvp(Avp.SERVICE_IDENTIFIER_CCA, mscc.getServiceIdentifier(), true, false);
                }
                if ((mscc.getGrantedServiceUnit() < 1) && ((request.getRequestTypeAVPValue() != RequestType.TERMINATION_REQUEST))) {
                    resultCode = CreditControlResultCode.DIAMETER_CREDIT_LIMIT_REACHED;
                }

                AvpSet gsuAvp = answerMSCC.addGroupedAvp(Avp.GRANTED_SERVICE_UNIT, true, false);
                gsuAvp.addAvp(Avp.CC_INPUT_OCTETS, 0L, true, false);
                gsuAvp.addAvp(Avp.CC_OUTPUT_OCTETS, 0L, true, false);

                if ((request.getRequestTypeAVPValue() == RequestType.TERMINATION_REQUEST) || (mscc.getGrantedServiceUnit() < 1)) {
                    LOG.info("Terminate");
                    // Since this is a terminate reply no service is granted
                    gsuAvp.addAvp(Avp.CC_TIME, 0, true, false);
                    gsuAvp.addAvp(Avp.CC_TOTAL_OCTETS, 0L, true, false);
                    gsuAvp.addAvp(Avp.CC_SERVICE_SPECIFIC_UNITS, 0L, true, false);

                    addFinalUnitAction(answerMSCC, mscc);
                } else {
                    gsuAvp.addAvp(Avp.CC_TOTAL_OCTETS, mscc.getGrantedServiceUnit(), true, false);
                }

                answerMSCC.addAvp(Avp.RESULT_CODE, resultCode, true, false);
                answerMSCC.addAvp(Avp.VALIDITY_TIME, mscc.getValidityTime(), true, false);
            }
            LOG.info("Credit-Control-Answer");
            DiameterUtilities.printAvps(ccaAvps);

        } catch (InternalException e) {
            LOG.error("Failed to convert to Credit-Control-Answer", e);
        }
        return answer;
    }

    private void addFinalUnitAction(AvpSet answerMSCC, MultipleServiceCreditControl mscc) {

        // There seems to be a possibility to do some whitelisting here by using RESTRICT_ACCESS
        // We should have a look at: https://tools.ietf.org/html/rfc4006#section-5.6.3

        Optional<FinalUnitIndication> finalUnitIndicationReply = Optional.ofNullable(mscc.getFinalUnitIndication());
        if (finalUnitIndicationReply.isPresent()) {
            AvpSet finalUnitIndication = answerMSCC.addGroupedAvp(Avp.FINAL_UNIT_INDICATION, true, false);
            finalUnitIndication.addAvp(Avp.FINAL_UNIT_ACTION, mscc.getFinalUnitIndication().getFinalUnitAction(), true, false);
        }

        //ToDo : Add support for the rest of the Final-Unit-Action
    }
}
