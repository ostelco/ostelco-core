package com.telenordigital.ocsgw;

import com.telenordigital.ocsgw.data.DataSource;
import com.telenordigital.ocsgw.data.DataSourceType;
import com.telenordigital.ocsgw.data.grpc.GrpcDataSource;
import com.telenordigital.ocsgw.data.local.LocalDataSource;
import com.telenordigital.ocsgw.utils.AppConfig;
import com.telenordigital.ostelco.diameter.CreditControlContext;
import org.jdiameter.api.*;
import org.jdiameter.api.auth.ServerAuthSession;
import org.jdiameter.api.auth.events.ReAuthRequest;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.common.impl.app.auth.AuthSessionFactoryImpl;
import org.jdiameter.common.impl.app.auth.ReAuthRequestImpl;
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

    //https://tools.ietf.org/html/rfc4006#page-30
    public void sendReAuthRequest(final String msisdn) {
        ISessionFactory sessionFactory = null;
        try {
            sessionFactory = (ISessionFactory) stack.getSessionFactory();
            ((ISessionFactory) sessionFactory).registerAppFacory(ServerAuthSession.class, new AuthSessionFactoryImpl(sessionFactory));
            ServerAuthSession authSession = sessionFactory.getNewAppSession("BadCustomSessionId;YesWeCanPassId;" + System.currentTimeMillis(),ApplicationId.createByAuthAppId(4L), ServerAuthSession.class);
            Request request = sessionFactory.getNewSession().createRequest(258, ApplicationId.createByAuthAppId(4L), stack.getMetaData().getLocalPeer().getRealmName());
            ReAuthRequest reAuthRequest = new ReAuthRequestImpl(request);
            authSession.sendReAuthRequest(reAuthRequest);
        } catch (IllegalDiameterStateException | InternalException | RouteException | OverloadException e) {
            LOG.warn("Failed to send Re-Auth Request");
        }
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
            default:
                LOG.warn("Unknow DataStoreType {}", appConfig.getDataStoreType());
                source = new LocalDataSource();
                break;
        }
        source.init();
    }
}
