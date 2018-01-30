package com.telenordigital.ext_pgw;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdiameter.api.*;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TestClient implements EventListener<Request, Answer> {

    private static final Logger log = Logger.getLogger(TestClient.class);
    // The request the test client will send
    private JCreditControlRequest request;
    // The result for the request
    private AvpSet resultAvps;
    // The resultcode AVP for the request
    private Avp resultCodeAvp;
    //configuration files
    private static final String configFile = "client-jdiameter-config.xml";
    // definition of codes, IDs
    private static final long applicationID = 4L;  // Diameter Credit Control Application (4)
    private ApplicationId authAppId = ApplicationId.createByAuthAppId(applicationID);
    //stack and session factory
    private Stack stack;
    private SessionFactory factory;
    private Session session;  // session used as handle for communication
    private boolean finished = false;  //boolean telling if we finished our interaction

    static {
        //configure logging.
        configLog4j();
    }

    public AvpSet getResultAvps() {
        return resultAvps;
    }
    public Avp getResultCodeAvp() {
        return resultCodeAvp;
    }

    private static void configLog4j() {
        InputStream inStreamLog4j = TestClient.class.getClassLoader().getResourceAsStream("log4j.properties");
        Properties propertiesLog4j = new Properties();
        try {
            propertiesLog4j.load(inStreamLog4j);
            PropertyConfigurator.configure(propertiesLog4j);
        } catch (Exception e) {
            log.error("Failed to configure Log4j", e);
        } finally {
            if(inStreamLog4j!=null)
            {
                try {
                    inStreamLog4j.close();
                } catch (IOException e) {
                    log.error("Failed to close InputStream", e);
                }
            }
        }
        log.debug("log4j configured");
    }

    public void initStack() {
        log.info("Initializing Stack...");
        try {
            this.stack = new StackImpl();
            Configuration config = getConfig();
            factory = stack.init(config);

            printApplicationInfo();

            //Register network req listener, even though we wont receive requests
            //this has to be done to inform stack that we support application
            Network network = stack.unwrap(Network.class);
            network.addNetworkReqListener(request -> {
                //this wontbe called.
                return null;
            }, this.authAppId); //passing our example app id.

        } catch (Exception e) {
            log.error("Failed to init Diameter Stack", e);
            if (this.stack != null) {
                this.stack.destroy();
            }
            return;
        }

        try {
            log.info("Starting stack");
            stack.start();
            log.info("Stack is running.");
        } catch (Exception e) {
            log.error("Failed to start Diameter Stack", e);
            stack.destroy();
            return;
        }
        log.info("Stack initialization successfully completed.");
    }

    private void printApplicationInfo() {
        //Print info about application
        Set<ApplicationId> appIds = stack.getMetaData().getLocalPeer().getCommonApplications();

        log.info("Diameter Stack  :: Supporting " + appIds.size() + " applications.");
        for (ApplicationId id : appIds) {
            log.info("Diameter Stack  :: Common :: " + id);
        }
    }

    private Configuration getConfig() {
        Configuration config = null;
        InputStream is = null;
        //Parse stack configuration
        is = this.getClass().getClassLoader().getResourceAsStream(configFile);
        try {
            config = new XMLConfiguration(is);
        } catch (Exception e) {
            log.error("Failed to load configuration", e);
        }

        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            log.error("Failed to close InputStream", e);
        }
        return config;
    }

    public boolean isAnswerReceived() {
        return this.finished;
    }

    public Session getSession() {
        return session;
    }

    public void start() {
        try {
            //wait for connection to peer
            Thread.currentThread().sleep(5000);
            this.session = this.factory.getNewSession("BadCustomSessionId;YesWeCanPassId;" + System.currentTimeMillis());
        } catch (InternalException | InterruptedException e) {
            log.error("Start Failed", e);
        }
    }

    public void sendNextRequest() {
        finished = false;
        try {
            this.session.send(request.getMessage(), this);
            dumpMessage(request.getMessage(), true); //dump info on console
        } catch (InternalException | IllegalDiameterStateException | RouteException| OverloadException e) {
            log.error("Failed to send request", e);
            finished = true;
        }
    }

    @Override
    public void receivedSuccessMessage(Request request, Answer answer) {
        dumpMessage(answer,false);
        resultAvps = answer.getAvps();
        resultCodeAvp = answer.getResultCode();
        this.finished = true;
    }

    @Override
    public void timeoutExpired(Request request) {
        log.info("Timeout expired" + request);
    }

    private void dumpMessage(Message message, boolean sending) {
        log.info((sending?"Sending ":"Received ") + (message.isRequest() ? "Request: " : "Answer: ") + message.getCommandCode() + "\nE2E:"
                + message.getEndToEndIdentifier() + "\nHBH:" + message.getHopByHopIdentifier() + "\nAppID:" + message.getApplicationId());
        log.info("AVPS["+message.getAvps().size()+"]: \n");
    }
    
    public void setRequest(JCreditControlRequest request) {
        this.request = request;
    }

    public static void main(String[] args) {
        TestClient ec = new TestClient();
        ec.initStack();
        ec.start();

        while (ec.isAnswerReceived()) {
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
                log.error("Application Interrupted", e);
            }
        }
    }

    public void shutdown() {
        try {
            stack.stop(0, TimeUnit.MILLISECONDS ,0);
        } catch (IllegalDiameterStateException | InternalException e) {
            log.error("Failed to shutdown", e);
        }
        stack.destroy();
    }
}