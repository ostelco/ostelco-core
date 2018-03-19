package org.ostelco.ocsgw;

import org.ostelco.ocsgw.data.DataSource;
import org.ostelco.ocsgw.data.DataSourceType;
import org.ostelco.ocsgw.data.grpc.GrpcDataSource;
import org.ostelco.ocsgw.data.local.LocalDataSource;
import org.ostelco.ocsgw.data.proxy.ProxyDataSource;
import org.ostelco.ocsgw.utils.AppConfig;
import org.ostelco.diameter.CreditControlContext;
import org.jdiameter.api.Stack;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OcsServer {

    private static final Logger LOG = LoggerFactory.getLogger(OcsApplication.class);
    private static final OcsServer INSTANCE = new OcsServer();
    private Stack stack;
    private DataSource source;

    public static OcsServer getInstance() {
        return INSTANCE;
    }

    private OcsServer() {
    }

    public synchronized void handleRequest(ServerCCASession session, JCreditControlRequest request) {

        final CreditControlContext ccrContext = new CreditControlContext(session, request);
        ccrContext.setOriginHost(stack.getMetaData().getLocalPeer().getUri().getFQDN());
        ccrContext.setOriginRealm(stack.getMetaData().getLocalPeer().getRealmName());
        source.handleRequest(ccrContext);
    }

    public void init(Stack stack, AppConfig appConfig) {
        this.stack = stack;

        switch (appConfig.getDataStoreType()) {
            case DataSourceType.GRPC:
                LOG.info("Using GrpcDataSource");
                source = new GrpcDataSource(appConfig.getGrpcServer(), appConfig.encryptGrpc());
                break;
            case DataSourceType.LOCAL:
                LOG.info("Using LocalDataSource");
                source = new LocalDataSource();
                break;
            case DataSourceType.PROXY:
                LOG.info("Using ProxyDataSource");
                GrpcDataSource secondary = new GrpcDataSource(appConfig.getGrpcServer(), appConfig.encryptGrpc());
                secondary.init();
                source = new ProxyDataSource(secondary);
                break;
            default:
                LOG.warn("Unknown DataStoreType {}", appConfig.getDataStoreType());
                source = new LocalDataSource();
                break;
        }
        source.init();
    }
}
