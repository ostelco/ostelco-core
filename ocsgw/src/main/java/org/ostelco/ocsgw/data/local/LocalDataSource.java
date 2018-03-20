package org.ostelco.ocsgw.data.local;

import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.cca.ServerCCASession;
import org.ostelco.diameter.CreditControlContext;
import org.ostelco.diameter.model.CreditControlAnswer;
import org.ostelco.diameter.model.FinalUnitAction;
import org.ostelco.diameter.model.FinalUnitIndication;
import org.ostelco.diameter.model.MultipleServiceCreditControl;
import org.ostelco.diameter.model.RedirectAddressType;
import org.ostelco.diameter.model.RedirectServer;
import org.ostelco.diameter.model.ServiceUnit;
import org.ostelco.ocs.api.CreditControlRequestType;
import org.ostelco.ocsgw.OcsServer;
import org.ostelco.ocsgw.data.DataSource;
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
        LOG.info("Sending Credit-Control-Answer");
        try {
            final ServerCCASession session = OcsServer.getInstance().getStack().getSession(context.getSessionId(), ServerCCASession.class);
            session.sendCreditControlAnswer(context.createCCA(answer));
        } catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException e) {
            LOG.error("Failed to send Credit-Control-Answer. SessionId : {}", context.getSessionId());
        }
    }

    public CreditControlAnswer createCreditControlAnswer(CreditControlContext context) {

        final List<MultipleServiceCreditControl> origMultipleServiceCreditControls = context.getCreditControlRequest().getMultipleServiceCreditControls();
        final List<MultipleServiceCreditControl> newMultipleServiceCreditControls = new ArrayList<>();

        for (MultipleServiceCreditControl mscc : origMultipleServiceCreditControls) {

            FinalUnitIndication finalUnitIndication = null;

            if (context.getOriginalCreditControlRequest().getRequestTypeAVPValue() == CreditControlRequestType.TERMINATION_REQUEST.getNumber()) {
                finalUnitIndication = new FinalUnitIndication(
                        FinalUnitAction.TERMINATE,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new RedirectServer(RedirectAddressType.IPV4_ADDRESS));
            }

            final List<ServiceUnit> newRequested = new ArrayList<>();
            for (ServiceUnit requested : mscc.getRequested()) {
                newRequested.add(new ServiceUnit(requested.getTotal(), 0, 0));
            }

            final ServiceUnit granted;
            if (!newRequested.isEmpty()) {
                granted = newRequested.get(0);
            } else {
                granted = new ServiceUnit(0, 0, 0);
            }

            MultipleServiceCreditControl newMscc = new MultipleServiceCreditControl(
                    mscc.getRatingGroup(),
                    mscc.getServiceIdentifier(),
                    newRequested,
                    new ServiceUnit(mscc.getUsed().getTotal(), mscc.getUsed().getInput(), mscc.getUsed().getOutput()),
                    granted,
                    mscc.getValidityTime(),
                    finalUnitIndication);

            newMultipleServiceCreditControls.add(newMscc);
        }

        return new CreditControlAnswer(newMultipleServiceCreditControls);
    }

    public boolean isBlocked(final String msisdn) {
        return false;
    }
}
