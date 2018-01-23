package com.telenordigital.ocsgw.data.local;

import com.telenordigital.ocsgw.diameter.*;
import com.telenordigital.ocsgw.data.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

/**
 * Local DataSource that will accept all Credit Control Requests
 * Can be used as a bypass.
 *
 */
public class LocalDataSource implements DataSource {

    private static final Logger logger = LoggerFactory.getLogger(LocalDataSource.class);

    @Override
    public void init() {
        // No init needed
    }

    @Override
    public void handleRequest(CreditControlRequestContext context) {
        CreditControlAnswer answer = createCreditControlAnswer(context);
        logger.info("Sending Credit-Control-Answer");
        context.sendCreditControlAnswer(answer);
    }

    private CreditControlAnswer createCreditControlAnswer(CreditControlRequestContext context) {
        CreditControlAnswer answer = new CreditControlAnswer();

        final LinkedList<MultipleServiceCreditControl> multipleServiceCreditControls = context.getCreditControlRequest().getMultipleServiceCreditControls();

        for (MultipleServiceCreditControl mscc : multipleServiceCreditControls) {
            mscc.setGrantedServiceUnit(mscc.getRequestedUnits());
        }

        answer.setMultipleServiceCreditControls(multipleServiceCreditControls);
        return answer;
    }
}
