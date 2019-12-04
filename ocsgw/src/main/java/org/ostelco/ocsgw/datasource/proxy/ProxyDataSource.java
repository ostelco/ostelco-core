package org.ostelco.ocsgw.datasource.proxy;

import org.ostelco.ocsgw.datasource.DataSource;
import org.ostelco.ocsgw.datasource.local.LocalDataSource;
import org.ostelco.diameter.CreditControlContext;
import org.ostelco.ocs.api.CreditControlRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy DataSource is a combination of the Local DataSource and any other
 * DataSource.
 *
 * With the proxy approach the CCR-I request will not use the LocalDataStore. If the
 * reply for the CCR-I is accepted the following CCR-U requests will use the LocalDataSource
 * to quickly reply and accept the query. But it will also send the same request on using the
 * secondary DataSource. When the secondary DataSource deny a CCR-U then the next request will be denied.
 *
 * The DataSource will keep a block list of msisdns that has failed to query new buckets when doing CCR.
 */
public class ProxyDataSource implements DataSource {

    private final DataSource local = new LocalDataSource();

    private final DataSource secondary;

    private static final Logger LOG = LoggerFactory.getLogger(ProxyDataSource.class);

    public ProxyDataSource(DataSource dataSource) {
        secondary = dataSource;
    }

    @Override
    public void init() {
        // No init needed
    }

    @Override
    public void handleRequest(CreditControlContext context) {

        switch (context.getOriginalCreditControlRequest().getRequestTypeAVPValue()) {
            case CreditControlRequestType.INITIAL_REQUEST_VALUE:
                secondary.handleRequest(context);
                break;
            case CreditControlRequestType.UPDATE_REQUEST_VALUE:
                if (!secondary.isBlocked(context.getCreditControlRequest().getMsisdn())) {
                    proxyAnswer(context);
                } else {
                    secondary.handleRequest(context);
                }
                break;
            case CreditControlRequestType.TERMINATION_REQUEST_VALUE:
                proxyAnswer(context);
                break;
            default:
                LOG.warn("Unknown request type : {}", context.getOriginalCreditControlRequest().getRequestTypeAVPValue());
        }
    }

    /**
     * Use the local data source to send an answer directly to P-GW.
     * Use secondary to report the usage to OCS.
     */
    private void proxyAnswer(CreditControlContext context) {
        local.handleRequest(context);
        context.setSkipAnswer(true);
        secondary.handleRequest(context);
    }

    @Override
    public boolean isBlocked(final String msisdn) {
        return secondary.isBlocked(msisdn);
    }
}
