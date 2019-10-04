package org.ostelco.ocsgw.datasource.local;

import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.cca.ServerCCASession;
import org.ostelco.diameter.CreditControlContext;
import org.ostelco.diameter.model.*;
import org.ostelco.ocs.api.CreditControlRequestType;
import org.ostelco.ocsgw.OcsServer;
import org.ostelco.ocsgw.datasource.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Local DataSource that will accept all Credit Control Requests
 * Can be used as a bypass.
 *
 */
public class LocalDataSource implements DataSource {

    private static final Logger LOG = LoggerFactory.getLogger(LocalDataSource.class);

    @Override
    public void init() {
        // No init needed
    }

    @Override
    public void handleRequest(CreditControlContext context) {
        CreditControlAnswer answer = createCreditControlAnswer(context);
        LOG.info("Got Credit-Control-Request [{}] [{}]", context.getCreditControlRequest().getMsisdn(), context.getSessionId());
        try {
            final ServerCCASession session = OcsServer.INSTANCE.getStack().getSession(context.getSessionId(), ServerCCASession.class);
            session.sendCreditControlAnswer(context.createCCA(answer));
            LOG.info("Sent Credit-Control-Answer [{}] [{}]", context.getCreditControlRequest().getMsisdn(), context.getSessionId());
        } catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException | NullPointerException e) {
            LOG.error("Failed to send Credit-Control-Answer. [{}] [{}]", context.getCreditControlRequest().getMsisdn(), context.getSessionId(), e);
        }
    }

    private CreditControlAnswer createCreditControlAnswer(CreditControlContext context) {

        final List<MultipleServiceCreditControl> origMultipleServiceCreditControls = context.getCreditControlRequest().getMultipleServiceCreditControls();
        final List<MultipleServiceCreditControl> newMultipleServiceCreditControls = new ArrayList<>();

        for (MultipleServiceCreditControl mscc : origMultipleServiceCreditControls) {

            FinalUnitIndication finalUnitIndication = null;

            if (context.getOriginalCreditControlRequest().getRequestTypeAVPValue() == CreditControlRequestType.TERMINATION_REQUEST.getNumber()) {
                finalUnitIndication = new FinalUnitIndication(
                        FinalUnitAction.TERMINATE,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new RedirectServer(
                                RedirectAddressType.IPV4_ADDRESS,
                                "")
                );
            }

            final List<ServiceUnit> newRequested = new ArrayList<>();
            for (ServiceUnit requested : mscc.getRequested()) {
                newRequested.add(new ServiceUnit(requested.getTotal(), 0, 0));
            }

            if (!newRequested.isEmpty()) {
                final ServiceUnit granted = newRequested.get(0);
                MultipleServiceCreditControl newMscc = new MultipleServiceCreditControl(
                        mscc.getRatingGroup(),
                        mscc.getServiceIdentifier(),
                        newRequested,
                        mscc.getUsed(),
                        granted,
                        mscc.getValidityTime(),
                        7200,
                        (long) (granted.getTotal() * 0.2), // 20%
                        finalUnitIndication,
                        ResultCode.DIAMETER_SUCCESS);

                newMultipleServiceCreditControls.add(newMscc);

            }
        }

        int validityTime = 0;
        if (newMultipleServiceCreditControls.isEmpty()) {
            validityTime = 86400;
        }

        return new CreditControlAnswer(ResultCode.DIAMETER_SUCCESS, newMultipleServiceCreditControls, validityTime);
    }

    public boolean isBlocked(final String msisdn) {
        return false;
    }
}
