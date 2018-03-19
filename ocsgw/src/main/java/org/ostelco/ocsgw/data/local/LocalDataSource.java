package org.ostelco.ocsgw.data.local;

import org.ostelco.ocsgw.data.DataSource;
import org.ostelco.diameter.CreditControlContext;
import org.ostelco.diameter.model.CreditControlAnswer;
import org.ostelco.diameter.model.FinalUnitAction;
import org.ostelco.diameter.model.FinalUnitIndication;
import org.ostelco.diameter.model.MultipleServiceCreditControl;
import org.ostelco.diameter.model.RedirectAddressType;
import org.ostelco.diameter.model.RedirectServer;
import org.ostelco.diameter.model.ServiceUnit;
import org.ostelco.ocs.api.CreditControlRequestType;
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
        context.sendCreditControlAnswer(answer);
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

            MultipleServiceCreditControl newMscc = new MultipleServiceCreditControl(
                    mscc.getRatingGroup(),
                    mscc.getServiceIdentifier(),
                    new ServiceUnit(mscc.getRequested().getTotal(),0,0),
                    new ServiceUnit(mscc.getUsed().getTotal(),mscc.getUsed().getInput(),mscc.getUsed().getOutput()),
                    new ServiceUnit(mscc.getRequested().getTotal(),0,0), // granted Service Unit
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
