package org.ostelco.ocsgw;

import com.google.cloud.storage.*;
import org.jdiameter.api.*;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.client.api.ISessionFactory;
import org.jdiameter.common.impl.app.cca.CCASessionFactoryImpl;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.app.cca.ServerCCASessionImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;
import org.ostelco.diameter.model.RequestType;
import org.ostelco.ocsgw.utils.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class OcsApplication extends CCASessionFactoryImpl implements NetworkReqListener {

    private static final Logger LOG = LoggerFactory.getLogger(OcsApplication.class);
    private static final String DIAMETER_CONFIG_FILE = "server-jdiameter-config.xml";
    private static final String CONFIG_FOLDER = "/config/";
    private static final long APPLICATION_ID = 4L;  // Diameter Credit Control Application (4)
    private static final long VENDOR_ID_3GPP = 10415;
    private static Stack stack = null;

    public static void main(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread(OcsApplication::shutdown));

        OcsApplication app = new OcsApplication();

        String configFile = System.getenv("DIAMETER_CONFIG_FILE");
        if (configFile == null) {
            configFile = DIAMETER_CONFIG_FILE;
        }

        String configFolder = System.getenv("CONFIG_FOLDER");
        if (configFolder == null) {
            configFolder = CONFIG_FOLDER;
        }

        app.start(configFolder, configFile);
    }


    public static void shutdown() {
        LOG.info("Shutting down OcsApplication...");
        if (stack != null) {
            try {
                stack.stop(30000, TimeUnit.MILLISECONDS , DisconnectCause.REBOOTING);
            } catch (IllegalDiameterStateException | InternalException e) {
                LOG.error("Failed to gracefully shutdown OcsApplication", e);
            }
            stack.destroy();
        }
    }

    private void fetchConfig(final String configDir, final String configFile) {
        final String vpcEnv = System.getenv("VPC_ENV");
        final String instance = System.getenv("INSTANCE");
        final String serviceFile = System.getenv("SERVICE_FILE");

        if ((vpcEnv != null) && (instance != null)) {
            String bucketName = "ocsgw-" + vpcEnv + "-" + instance + "-bucket";

            fetchFromStorage(configFile, configDir, bucketName);
            fetchFromStorage(serviceFile, configDir, bucketName);
        }
    }

    private void fetchFromStorage(String fileName, String configDir, String bucketName) {
        if (fileName == null) {
            return;
        }

        LOG.debug("Downloading file : " + fileName);

        Storage storage = StorageOptions.getDefaultInstance().getService();
        Blob blobFile = storage.get(BlobId.of(bucketName, fileName));
        final Path destFilePath = Paths.get(configDir + "/" + fileName);
        blobFile.downloadTo(destFilePath);
    }


    public void start(final String configDir, final String configFile) {
        try {

            fetchConfig(configDir, configFile);

            Configuration diameterConfig = new XMLConfiguration(configDir +  configFile);
            stack = new StackImpl();
            sessionFactory = (ISessionFactory) stack.init(diameterConfig);

            OcsServer.INSTANCE.init$ocsgw(stack, new AppConfig());

            Network network = stack.unwrap(Network.class);
            network.addNetworkReqListener(this, ApplicationId.createByAuthAppId(0L, APPLICATION_ID));
            network.addNetworkReqListener(this, ApplicationId.createByAuthAppId(VENDOR_ID_3GPP, APPLICATION_ID));

            stack.start(Mode.ALL_PEERS, 30000, TimeUnit.MILLISECONDS);

            init(sessionFactory);
            sessionFactory.registerAppFacory(ServerCCASession.class, this);
            printAppIds();

        } catch (Exception e) {
            LOG.error("Failure initializing OcsApplication", e);
        }
    }

    @Override
    public Answer processRequest(Request request) {
        LOG.debug("[<<] Received Request [{}]", request.getSessionId());
        try {
            ServerCCASessionImpl session = sessionFactory.getNewAppSession(request.getSessionId(), ApplicationId.createByAuthAppId(4L), ServerCCASession.class);
            session.processRequest(request);
            LOG.debug("processRequest finished [{}]", request.getSessionId());
        }
        catch (InternalException e) {
            LOG.error("[><] Failure handling received request.", e);
        }

        return null;
    }

    @Override
    public void doCreditControlRequest(ServerCCASession session, JCreditControlRequest request) {

        switch (request.getRequestTypeAVPValue()) {
            case RequestType.INITIAL_REQUEST:
            case RequestType.UPDATE_REQUEST:
            case RequestType.TERMINATION_REQUEST:
                LOG.info("[<<] Received Credit-Control-Request from P-GW [ {} ] [{}]", RequestType.getTypeAsString(request.getRequestTypeAVPValue()), session.getSessionId());
                try {
                    OcsServer.INSTANCE.handleRequest$ocsgw(session, request);
                } catch (Exception e) {
                    LOG.error("[><] Failure processing Credit-Control-Request [" + RequestType.getTypeAsString(request.getRequestTypeAVPValue()) + "] + [session.getSessionId()]", e);
                }
                break;
            case RequestType.EVENT_REQUEST:
                LOG.info("[<<] Received Credit-Control-Request [EVENT]");
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

        LOG.info("Uri : " + stack.getMetaData().getLocalPeer().getUri());
        LOG.info("Realm : " + stack.getMetaData().getLocalPeer().getRealmName());
        LOG.info("IP : " + Arrays.toString(stack.getMetaData().getLocalPeer().getIPAddresses()));
    }
}
