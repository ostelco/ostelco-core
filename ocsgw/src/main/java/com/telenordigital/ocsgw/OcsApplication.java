package com.telenordigital.ocsgw;

import com.telenordigital.ocsgw.diameter.RequestType;
import com.telenordigital.ocsgw.utils.AppConfig;
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Mode;
import org.jdiameter.api.Network;
import org.jdiameter.api.NetworkReqListener;
import org.jdiameter.api.Request;
import org.jdiameter.api.Stack;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.common.impl.app.cca.CCASessionFactoryImpl;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.app.cca.ServerCCASessionImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class OcsApplication extends CCASessionFactoryImpl implements NetworkReqListener {

    private static final Logger LOG = LoggerFactory.getLogger(OcsApplication.class);
    private static final String DIAMETER_CONFIG_FILE = "server-jdiameter-config.xml";
    private static final long APPLICATION_ID = 4L;  // Diameter Credit Control Application (4)
    private Stack stack = null;

    public static void main(String[] args) {

        OcsApplication app = new OcsApplication();
        app.start();
    }

    public void start() {
        try {
            InputStream iStream = this.getClass().getClassLoader().getResourceAsStream(DIAMETER_CONFIG_FILE);
            Configuration diameterConfig = new XMLConfiguration(iStream);
            iStream.close();
            stack = new StackImpl();
            stack.init(diameterConfig);

            OcsServer.getInstance().init(stack, new AppConfig());

            Network network = stack.unwrap(Network.class);
            network.addNetworkReqListener(this, ApplicationId.createByAuthAppId(APPLICATION_ID));

            stack.start(Mode.ALL_PEERS, 30000, TimeUnit.MILLISECONDS);

            sessionFactory = (ISessionFactory) stack.getSessionFactory();
            init(sessionFactory);
            sessionFactory.registerAppFacory(ServerCCASession.class, this);

            printAppIds();

        } catch (Exception e) {
            LOG.error("Failure initializing OcsApplication", e);
        }
    }

    @Override
    public Answer processRequest(Request request) {
        LOG.info("<< Received Request");
        try {
            ServerCCASessionImpl session =
                    (sessionFactory).getNewAppSession(request.getSessionId(), ApplicationId.createByAuthAppId(4L), ServerCCASession.class);
            session.processRequest(request);
        }
        catch (InternalException e) {
            LOG.error(">< Failure handling received request.", e);
        }

        return null;
    }

    @Override
    public void doCreditControlRequest(ServerCCASession session, JCreditControlRequest request) {

        switch (request.getRequestTypeAVPValue()) {
            case RequestType.INITIAL_REQUEST:
            case RequestType.UPDATE_REQUEST:
            case RequestType.TERMINATION_REQUEST:
                LOG.info("<< Received Credit-Control-Request [ {} ]", RequestType.getTypeAsString(request.getRequestTypeAVPValue()));
                try {
                    OcsServer.getInstance().handleRequest(session, request);
                } catch (Exception e) {
                    LOG.error(">< Failure processing Credit-Control-Request [" + RequestType.getTypeAsString(request.getRequestTypeAVPValue()) + "]", e);
                }
                break;
            case RequestType.EVENT_REQUEST:
                LOG.info("<< Received Credit-Control-Request [EVENT]");
                break;
            default:
                break;
        }
    }


    private void printAppIds() {
        Set<ApplicationId> appIds = stack.getMetaData().getLocalPeer().getCommonApplications();

        LOG.info("Diameter Stack  :: Supporting {} applications.", appIds.size());
        for (ApplicationId id : appIds) {
            LOG.info("Diameter Stack  :: Common :: {}", id);
        }
    }
}
