package com.telenordigital.ocsgw.data.hybrid;

import com.telenordigital.ocsgw.data.DataSource;
import com.telenordigital.ocsgw.data.local.LocalDataSource;
import com.telenordigital.ostelco.diameter.CreditControlContext;
import com.telenordigital.prime.ocs.CreditControlRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hybrid DataSource is a combination of the Local datasource and any other
 * DataSource.
 *
 * With the hybrid approach the CCR-I request will not use the LocalDataStore. If the
 * reply for the CCR-I is accepted the following CCR-U requests will use the LocalDataSource
 * to quickly reply and accept the query. But it will also send the same request on using the
 * secondary DataSource. When the secondary DataSource deny a CCR-U then the next request will be denied.
 *
 */
public class HybridDataSource implements DataSource {

    private static final Logger LOG = LoggerFactory.getLogger(HybridDataSource.class);

    private final DataSource local = new LocalDataSource();

    private DataSource secondary;

    public void setSecondaryDataSource(DataSource dataSource) {
        secondary = dataSource;
    }

    @Override
    public void init() {
        // No init needed
    }

    @Override
    public void handleRequest(CreditControlContext context) {
        if (context.getOriginalCreditControlRequest().getRequestTypeAVPValue() != CreditControlRequestType.UPDATE_REQUEST.getNumber()) {
            local.handleRequest(context);
            secondary.handleRequest(context);
        } else {
            if (!secondary.isBlocked(context.getCreditControlRequest().getMsisdn())) {
                local.handleRequest(context);
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
