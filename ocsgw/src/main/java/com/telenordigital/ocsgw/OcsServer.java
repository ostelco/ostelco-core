package com.telenordigital.ocsgw;

import com.telenordigital.ocsgw.data.grpc.GrpcDataSource;
import com.telenordigital.ocsgw.diameter.*;
import com.telenordigital.ocsgw.data.DataSource;
import com.telenordigital.ocsgw.data.DataSourceType;
import com.telenordigital.ocsgw.data.local.LocalDataSource;
import com.telenordigital.ocsgw.utils.AppConfig;
import org.jdiameter.api.*;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OcsServer {

    private static final Logger logger = LoggerFactory.getLogger(OcsApplication.class);
    private static final OcsServer INSTANCE = new OcsServer();
    private Stack stack;
    private DataSource source;

    public static OcsServer getInstance() {
        return INSTANCE;
    }

    private OcsServer() {
    }

    public synchronized void handleRequest(ServerCCASession session, JCreditControlRequest request) {

        final CreditControlRequestContext ccrContext = new CreditControlRequestContext(session, request);
        ccrContext.setOriginHost(stack.getMetaData().getLocalPeer().getUri().getFQDN());
        ccrContext.setOriginRealm(stack.getMetaData().getLocalPeer().getRealmName());
        source.handleRequest(ccrContext);
    }

    public void init(Stack stack, AppConfig appConfig) {
        this.stack = stack;

        switch (appConfig.getDataStoreType()) {
            case DataSourceType.GRPC:
                source = new GrpcDataSource();
                break;
            case DataSourceType.LOCAL:
                source = new LocalDataSource();
                break;
            default:
                logger.warn("Unknow DataStoreType " + appConfig.getDataStoreType());
                source = new LocalDataSource();
                break;
        }
        source.init();
    }
}
