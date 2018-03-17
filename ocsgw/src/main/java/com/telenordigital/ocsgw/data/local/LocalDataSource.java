package com.telenordigital.ocsgw.data.local;

import com.telenordigital.ocsgw.data.DataSource;
import com.telenordigital.ostelco.diameter.CreditControlContext;
import com.telenordigital.ostelco.diameter.model.CreditControlAnswer;
import com.telenordigital.ostelco.diameter.model.FinalUnitAction;
import com.telenordigital.ostelco.diameter.model.FinalUnitIndication;
import com.telenordigital.ostelco.diameter.model.MultipleServiceCreditControl;
import com.telenordigital.ostelco.diameter.model.RedirectAddressType;
import com.telenordigital.ostelco.diameter.model.RedirectServer;
import com.telenordigital.ostelco.diameter.model.ServiceUnit;
import com.telenordigital.prime.ocs.CreditControlRequestType;
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
}
