package com.telenordigital.ocsgw;

import com.telenordigital.ocsgw.data.DataSource;
import com.telenordigital.ocsgw.data.DataSourceType;
import com.telenordigital.ocsgw.data.grpc.GrpcDataSource;
import com.telenordigital.ocsgw.data.local.LocalDataSource;
import com.telenordigital.ocsgw.utils.AppConfig;
import com.telenordigital.ostelco.diameter.CreditControlContext;
import com.telenordigital.ostelco.diameter.model.ReAuthRequestType;
import org.jdiameter.api.*;
import org.jdiameter.api.auth.ServerAuthSession;
import org.jdiameter.api.auth.events.ReAuthRequest;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.client.impl.parser.MessageImpl;
import org.jdiameter.common.impl.app.auth.AuthSessionFactoryImpl;
import org.jdiameter.common.impl.app.auth.ReAuthRequestImpl;
import org.jdiameter.server.impl.app.cca.ServerCCASessionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;


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

        final CreditControlContext ccrContext = new CreditControlContext(session.getSessionId(), request);
        source.handleRequest(ccrContext);
    }

    public Stack getStack() {
        return stack;
    }

    //https://tools.ietf.org/html/rfc4006#page-30
    //https://tools.ietf.org/html/rfc3588#page-101
    public void sendReAuthRequest(final String sessionId, final String originHost, final String originRealm, final String destinationHost, final String destinationRealm) {
        try {
            ServerCCASessionImpl ccaSession = stack.getSession(sessionId, ServerCCASessionImpl.class);
            if (ccaSession != null && ccaSession.isValid()) {
                for (Session session : ccaSession.getSessions() ) {
                    Request request = session.createRequest(258, ApplicationId.createByAuthAppId(4L), destinationRealm, destinationHost);
                    AvpSet avps = request.getAvps();
                    avps.addAvp(Avp.RE_AUTH_REQUEST_TYPE, ReAuthRequestType.AUTHORIZE_ONLY.getValue());
                    avps.addAvp(Avp.ORIGIN_HOST, originHost, true, false, true);
                    avps.addAvp(Avp.ORIGIN_REALM, originRealm, true, false, true);
                    ReAuthRequest reAuthRequest = new ReAuthRequestImpl(request);
                    ccaSession.sendReAuthRequest(reAuthRequest);
                }
            } else {
                LOG.info("No session with ID {}", sessionId);
            }
        } catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException e) {
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
