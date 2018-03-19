package com.telenordigital.ocsgw.data.proxy;

import com.telenordigital.ocsgw.data.DataSource;
import com.telenordigital.ocsgw.data.local.LocalDataSource;
import com.telenordigital.ostelco.diameter.CreditControlContext;
import com.telenordigital.prime.ocs.CreditControlRequestType;
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

    private static final Logger LOG = LoggerFactory.getLogger(ProxyDataSource.class);

    private final DataSource local = new LocalDataSource();

    private DataSource secondary;

    public ProxyDataSource(DataSource dataSource) {
        secondary = dataSource;
    }

    @Override
    public void init() {
        // No init needed
    }

    @Override
    public void handleRequest(CreditControlContext context) {
        // CCR-I and CCR-T will always be handled by the secondary DataSource
        if (context.getOriginalCreditControlRequest().getRequestTypeAVPValue() != CreditControlRequestType.UPDATE_REQUEST.getNumber()) {
            secondary.handleRequest(context);
        } else {
            // For CCR-U we will send all requests to both Local and Secondary until the secondary has blocked the msisdn
            if (!secondary.isBlocked(context.getCreditControlRequest().getMsisdn())) {
                local.handleRequest(context);
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
