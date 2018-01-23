
package com.telenordigital.ocsgw.testclient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdiameter.api.Answer;
import org.jdiameter.api.ApplicationId;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Configuration;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.Message;
import org.jdiameter.api.MetaData;
import org.jdiameter.api.Network;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.Request;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.Session;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.Stack;
import org.jdiameter.api.StackType;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;

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
    private boolean finished = true;  //boolean telling if we finished our interaction

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
        if (log.isInfoEnabled()) {
            log.info("Initializing Stack...");
        }
        InputStream is = null;
        try {
            this.stack = new StackImpl();
            //Parse stack configuration
            is = this.getClass().getClassLoader().getResourceAsStream(configFile);
            Configuration config = new XMLConfiguration(is);
            factory = stack.init(config);
            if (log.isInfoEnabled()) {
                log.info("Client Stack Configuration successfully loaded.");
            }
            //Print info about applicatio
            Set<ApplicationId> appIds = stack.getMetaData().getLocalPeer().getCommonApplications();

            log.info("Diameter Stack  :: Supporting " + appIds.size() + " applications.");
            for (ApplicationId id : appIds) {
                log.info("Diameter Stack  :: Common :: " + id);
            }
            is.close();
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

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e1) {
                    log.error("Failed to close InputStream", e);
                }
            }
            return;
        }

        MetaData metaData = stack.getMetaData();
        //ignore for now.
        if (metaData.getStackType() != StackType.TYPE_SERVER || metaData.getMinorVersion() <= 0) {
            stack.destroy();
            if (log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                log.error("Incorrect driver");
            }
            return;
        }

        try {
            if (log.isInfoEnabled()) {
                log.info("Starting stack");
            }
            stack.start();
            if (log.isInfoEnabled()) {
                log.info("Stack is running.");
            }
        } catch (Exception e) {
            log.error("Failed to start Diameter Stack", e);
            stack.destroy();
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("Stack initialization successfully completed.");
        }
    }

    public boolean isAnswerReceived() {
        return !this.finished;
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
        if (log.isInfoEnabled()) {
            log.info((sending?"Sending ":"Received ") + (message.isRequest() ? "Request: " : "Answer: ") + message.getCommandCode() + "\nE2E:"
                    + message.getEndToEndIdentifier() + "\nHBH:" + message.getHopByHopIdentifier() + "\nAppID:" + message.getApplicationId());
            log.info("AVPS["+message.getAvps().size()+"]: \n");
        }
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