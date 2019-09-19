package org.ostelco.ocsgw.datasource.multi;

import org.ostelco.diameter.CreditControlContext;
import org.ostelco.ocs.api.CreditControlRequestType;
import org.ostelco.ocsgw.datasource.DataSource;

/**
 * Proxy DataSource is a combination of multiple DataSources
 *
 * With this approach the CCR-I, CCR-U and CCR-T can use different DataSources
 */
public class MultiDataSource implements DataSource {

    private DataSource initDatasource = null;
    private DataSource updateDatasource = null;
    private DataSource terminateDatasource = null;

    public MultiDataSource(DataSource initDatasource, DataSource updateDatasource, DataSource terminateDatasource) {
        this.initDatasource = initDatasource;
        this.updateDatasource = updateDatasource;
        this.terminateDatasource = terminateDatasource;
    }

    @Override
    public void init() {
        initDatasource.init();
        updateDatasource.init();
        terminateDatasource.init();
    }

    @Override
    public void handleRequest(CreditControlContext context) {

        if (context.getOriginalCreditControlRequest().getRequestTypeAVPValue()
                == CreditControlRequestType.INITIAL_REQUEST.getNumber()) {
            initDatasource.handleRequest(context);
        } else if (context.getOriginalCreditControlRequest().getRequestTypeAVPValue()
                == CreditControlRequestType.UPDATE_REQUEST.getNumber()) {
            updateDatasource.handleRequest(context);
        } else if (context.getOriginalCreditControlRequest().getRequestTypeAVPValue()
                == CreditControlRequestType.TERMINATION_REQUEST.getNumber()) {
            terminateDatasource.handleRequest(context);
        }
    }

    @Override
    public boolean isBlocked(final String msisdn) {
        return updateDatasource.isBlocked(msisdn);
    }
}
