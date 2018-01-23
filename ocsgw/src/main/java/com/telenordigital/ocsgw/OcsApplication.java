package com.telenordigital.ocsgw;

import com.telenordigital.ocsgw.utils.AppConfig;
import org.jdiameter.api.*;
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

import com.telenordigital.ocsgw.diameter.RequestType;

class OcsApplication extends CCASessionFactoryImpl implements NetworkReqListener {

    private static final Logger logger = LoggerFactory.getLogger(OcsApplication.class);
    private static final String diameterConfigFile = "server-jdiameter-config.xml";
    private static final long applicationID = 4L;  // Diameter Credit Control Application (4)

    public static void main(String[] args) {
        new OcsApplication();
    }

    public OcsApplication() {
        super();

        try {
            InputStream iStream = this.getClass().getClassLoader().getResourceAsStream(diameterConfigFile);
            Configuration diameterConfig = new XMLConfiguration(iStream);
            iStream.close();
            Stack stack = new StackImpl();
            stack.init(diameterConfig);

            OcsServer.getInstance().init(stack, new AppConfig());

            Network network = stack.unwrap(Network.class);
            network.addNetworkReqListener(this, ApplicationId.createByAuthAppId(applicationID));

            stack.start(Mode.ALL_PEERS, 30000, TimeUnit.MILLISECONDS);

            sessionFactory = (ISessionFactory) stack.getSessionFactory();
            init(sessionFactory);
            sessionFactory.registerAppFacory(ServerCCASession.class, this);

            Set<ApplicationId> appIds = stack.getMetaData().getLocalPeer().getCommonApplications();

            logger.info("Diameter Stack  :: Supporting " + appIds.size() + " applications.");
            for (ApplicationId id : appIds) {
                logger.info("Diameter Stack  :: Common :: " + id);
            }

        } catch (Exception e) {
            logger.error("Failure initializing OcsApplication", e);
        }
    }

    @Override
    public Answer processRequest(Request request) {
        logger.info("<< Received Request");
        try {
            ServerCCASessionImpl session =
                    (sessionFactory).getNewAppSession(request.getSessionId(), ApplicationId.createByAuthAppId(4L), ServerCCASession.class);
            session.processRequest(request);
        }
        catch (InternalException e) {
            logger.error(">< Failure handling received request.", e);
        }

        return null;
    }

    @Override
    public void doCreditControlRequest(ServerCCASession session, JCreditControlRequest request) {

        switch (request.getRequestTypeAVPValue()) {
            case RequestType.INITIAL_REQUEST:
            case RequestType.UPDATE_REQUEST:
            case RequestType.TERMINATION_REQUEST:
                logger.info("<< Received Credit-Control-Request [" + RequestType.getTypeAsString(request.getRequestTypeAVPValue()) + "]");
                try {
                    OcsServer.getInstance().handleRequest(session, request);
                } catch (Exception e) {
                    logger.error(">< Failure processing Credit-Control-Request [" + RequestType.getTypeAsString(request.getRequestTypeAVPValue()) + "]", e);
                }
                break;
            case RequestType.EVENT_REQUEST:
                logger.info("<< Received Credit-Control-Request [EVENT]");
                break;
            default:
                break;
        }
    }
}
