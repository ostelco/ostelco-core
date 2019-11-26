package org.ostelco.ocsgw.datasource.multi;

import org.ostelco.diameter.CreditControlContext;
import org.ostelco.ocs.api.CreditControlRequestType;
import org.ostelco.ocsgw.datasource.DataSource;
import org.ostelco.ocsgw.datasource.DataSourceOperations;

/**
 * Proxy DataSource is a combination of multiple DataSources
 *
 * With this approach the CCR-I, CCR-U and CCR-T can use different DataSources
 */
public class MultiDataSource implements DataSource {

    private DataSource initDatasource = null;
    private DataSource updateDatasource = null;
    private DataSource terminateDatasource = null;
    private DataSource activateDatasource = null;

    public MultiDataSource(DataSource initDatasource,
                           DataSource updateDatasource,
                           DataSource terminateDatasource,
                           DataSource activateDataSource) {
        this.initDatasource = initDatasource;
        this.updateDatasource = updateDatasource;
        this.terminateDatasource = terminateDatasource;
        this.activateDatasource = activateDataSource;
    }

    @Override
    public void init(DataSourceOperations dataSourceOperations) {
        initDatasource.init(DataSourceOperations.creditControl);
        updateDatasource.init(DataSourceOperations.creditControl);
        terminateDatasource.init(DataSourceOperations.creditControl);
        activateDatasource.init(DataSourceOperations.activate);
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
