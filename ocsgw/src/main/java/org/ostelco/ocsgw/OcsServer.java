package org.ostelco.ocsgw;

import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.Request;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.Session;
import org.jdiameter.api.Stack;
import org.jdiameter.api.auth.events.ReAuthRequest;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.common.impl.app.auth.ReAuthRequestImpl;
import org.jdiameter.server.impl.app.cca.ServerCCASessionImpl;
import org.ostelco.diameter.CreditControlContext;
import org.ostelco.diameter.model.SessionContext;
import org.ostelco.diameter.model.ReAuthRequestType;
import org.ostelco.ocsgw.data.DataSource;
import org.ostelco.ocsgw.data.DataSourceType;
import org.ostelco.ocsgw.data.grpc.GrpcDataSource;
import org.ostelco.ocsgw.data.local.LocalDataSource;
import org.ostelco.ocsgw.data.proxy.ProxyDataSource;
import org.ostelco.ocsgw.utils.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


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

        final CreditControlContext ccrContext = new CreditControlContext(
                session.getSessionId(),
                request,
                stack.getMetaData().getLocalPeer().getUri().getFQDN(),
                stack.getMetaData().getLocalPeer().getRealmName()
        );
        source.handleRequest(ccrContext);
    }

    public Stack getStack() {
        return stack;
    }

    //https://tools.ietf.org/html/rfc4006#page-30
    //https://tools.ietf.org/html/rfc3588#page-101
    public void sendReAuthRequest(final SessionContext sessionContext) {
        try {
            ServerCCASessionImpl ccaSession = stack.getSession(sessionContext.getSessionId(), ServerCCASessionImpl.class);
            if (ccaSession != null && ccaSession.isValid()) {
                // TODO martin: Not sure why there are multiple sessions for one session Id.
                for (Session session : ccaSession.getSessions()) {
                    if (session.isValid()) {
                        Request request = session.createRequest(258,
                                ApplicationId.createByAuthAppId(4L),
                                sessionContext.getOriginRealm(),
                                sessionContext.getOriginHost()
                        );
                        AvpSet avps = request.getAvps();
                        avps.addAvp(Avp.RE_AUTH_REQUEST_TYPE, ReAuthRequestType.AUTHORIZE_ONLY.ordinal(), true, false);
                        ReAuthRequest reAuthRequest = new ReAuthRequestImpl(request);
                        ccaSession.sendReAuthRequest(reAuthRequest);
                    } else {
                        LOG.info("Invalid session");
                    }
                }
            } else {
                LOG.info("No session with ID {}", sessionContext.getSessionId());
            }
        } catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException e) {
            LOG.warn("Failed to send Re-Auth Request", e);
        }
    }

    public void init(Stack stack, AppConfig appConfig) throws IOException {
        this.stack = stack;

        switch (appConfig.getDataStoreType()) {
            case DataSourceType.GRPC:
                LOG.info("Using GrpcDataSource");
                source = new GrpcDataSource(appConfig.getGrpcServer(), appConfig.getMetricsServer());
                break;
            case DataSourceType.LOCAL:
                LOG.info("Using LocalDataSource");
                source = new LocalDataSource();
                break;
            case DataSourceType.PROXY:
                LOG.info("Using ProxyDataSource");
                GrpcDataSource secondary = new GrpcDataSource(appConfig.getGrpcServer(), appConfig.getMetricsServer());
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
