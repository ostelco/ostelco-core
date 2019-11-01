package org.ostelco.ocsgw.datasource.proxy;

import org.ostelco.ocsgw.datasource.DataSource;
import org.ostelco.ocsgw.datasource.DataSourceOperations;
import org.ostelco.ocsgw.datasource.local.LocalDataSource;
import org.ostelco.diameter.CreditControlContext;
import org.ostelco.ocs.api.CreditControlRequestType;

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

    public ProxyDataSource(DataSource dataSource) {
        secondary = dataSource;
    }

    @Override
    public void init(DataSourceOperations operations) {
        local.init(operations);
    }

    @Override
    public void handleRequest(CreditControlContext context) {
        // CCR-I and CCR-T will always be handled by the secondary DataSource
        if (context.getOriginalCreditControlRequest().getRequestTypeAVPValue()
                != CreditControlRequestType.UPDATE_REQUEST.getNumber()) {
            secondary.handleRequest(context);
        } else {
            // For CCR-U we will send all requests to both Local and Secondary until the secondary has blocked the msisdn
            if (!secondary.isBlocked(context.getCreditControlRequest().getMsisdn())) {
                local.handleRequest(context);
                // When local datasource will be responding with Answer, Secondary datasource should skip to send Answer to P-GW.
                context.setSkipAnswer(true);
                secondary.handleRequest(context);
            } else {
                secondary.handleRequest(context);
            }
        }
    }

    @Override
    public boolean isBlocked(final String msisdn) {
        return secondary.isBlocked(msisdn);
    }
}
