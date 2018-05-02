package org.ostelco.diameter.test

import org.jdiameter.api.*
import org.jdiameter.common.impl.app.cca.JCreditControlRequestImpl
import org.jdiameter.server.impl.StackImpl
import org.jdiameter.server.impl.helpers.XMLConfiguration
import org.ostelco.diameter.logger
import org.ostelco.diameter.util.DiameterUtilities
import java.util.concurrent.TimeUnit


class TestClient : EventListener<Request, Answer> {

    private val LOG by logger()

    companion object {

        //configuration files
        private const val configFile = "client-jdiameter-config.xml"

        // definition of codes, IDs
        private const val applicationID = 4L  // Diameter Credit Control Application (4)

        private const val commandCode = 272 // Credit-Control
    }

    // The result for the request
    var resultAvps: AvpSet? = null
        private set

    // The resultcode AVP for the request
    var resultCodeAvp: Avp? = null
        private set

    private val authAppId = ApplicationId.createByAuthAppId(applicationID)

    // Diameter stack
    private lateinit var stack: Stack

    // session factory
    private lateinit var factory: SessionFactory

    // set if an answer to a Request has been received
    var isAnswerReceived = false
        private set

    // set if a request has been received
    var isRequestReceived = false
        private set

    // Parse stack configuration
    private lateinit var config: Configuration

    /**
     * Setup Diameter Stack
     *
     * @param configPath path to the jDiameter configuration file
     */
    fun initStack(configPath: String) {
        try {
            config = XMLConfiguration(configPath + configFile)
        } catch (e: Exception) {
            LOG.error("Failed to load configuration", e)
        }

        LOG.info("Initializing Stack...")
        try {
            this.stack = StackImpl()
            factory = stack.init(config)

            printApplicationInfo()

            //Register network req listener for Re-Auth-Requests
            val network = stack.unwrap<Network>(Network::class.java)
            network.addNetworkReqListener(
                    NetworkReqListener { request ->
                        LOG.info("Got a request")
                        resultAvps = request.getAvps()
                        DiameterUtilities().printAvps(resultAvps)
                        isRequestReceived = true
                        null
                    },
                    this.authAppId) //passing our example app id.

        } catch (e: Exception) {
            LOG.error("Failed to init Diameter Stack", e)
            this.stack.destroy()
            return
        }

        try {
            LOG.info("Starting stack")
            stack.start(Mode.ANY_PEER, 30000, TimeUnit.MILLISECONDS)
            LOG.info("Stack is running.")
        } catch (e: Exception) {
            LOG.error("Failed to start Diameter Stack", e)
            stack.destroy()
            return
        }

        LOG.info("Stack initialization successfully completed.")
    }

    private fun printApplicationInfo() {
        val appIds = stack.metaData.localPeer.commonApplications

        LOG.info("Diameter Stack  :: Supporting " + appIds.size + " applications.")
        for (id in appIds) {
            LOG.info("Diameter Stack  :: Common :: $id")
        }
    }

    /**
     * Reset Request test
     */
    fun initRequestTest() {
        this.isRequestReceived = false
    }

    /**
     * Create a new Request for the current Session
     *
     * @param destinationRealm Destination Realm
     * @param destinationHost Destination Host
     */
    fun createRequest(destinationRealm : String, destinationHost : String, session : Session): Request? {
        return session.createRequest(
                commandCode,
                ApplicationId.createByAuthAppId(applicationID),
                destinationRealm,
                destinationHost
        );
    }

    /**
     * Create a new DIAMETER session
     */
    fun createSession() : Session? {
        try {
            // FixMe : Need better way to make sure the session can be created
            if (!stack.isActive) {
                LOG.warn("Stack not active")
            }
            return this.factory.getNewSession("BadCustomSessionId;" + System.currentTimeMillis() + ";0")
        } catch (e: InternalException) {
            LOG.error("Start Failed", e)
        } catch (e: InterruptedException) {
            LOG.error("Start Failed", e)
        }
        return null
    }

    /**
     * Sends the next request using the current Session.
     *
     * @param request Request to send
     * @return false if send failed
     */
    fun sendNextRequest(request: Request, session: Session?): Boolean {
        isAnswerReceived = false
        if (session != null) {
            val ccr = JCreditControlRequestImpl(request)
            try {
                session.send(ccr.message, this)
                dumpMessage(ccr.message, true) //dump info on console
                return true
            } catch (e: InternalException) {
                LOG.error("Failed to send request", e)
            } catch (e: IllegalDiameterStateException) {
                LOG.error("Failed to send request", e)
            } catch (e: RouteException) {
                LOG.error("Failed to send request", e)
            } catch (e: OverloadException) {
                LOG.error("Failed to send request", e)
            }
        } else {
            LOG.error("Failed to send request. No session")
        }
        return false
    }

    override fun receivedSuccessMessage(request: Request, answer: Answer) {
        dumpMessage(answer, false)
        resultAvps = answer.avps
        resultCodeAvp = answer.resultCode
        this.isAnswerReceived = true
    }

    override fun timeoutExpired(request: Request) {
        LOG.info("Timeout expired $request")
    }


    private fun dumpMessage(message: Message, sending: Boolean) {
        LOG.info((if (sending) "Sending " else "Received ")
                + (if (message.isRequest) "Request: " else "Answer: ") + message.commandCode
                + "\nE2E:" + message.endToEndIdentifier
                + "\nHBH:" + message.hopByHopIdentifier
                + "\nAppID:" + message.applicationId)

        LOG.info("AVPS[" + message.avps.size() + "]: \n")
    }

    /**
     * Shut down the Diameter Stack
     */
    fun shutdown() {
        try {
            stack.stop(0, TimeUnit.MILLISECONDS, 0)
        } catch (e: IllegalDiameterStateException) {
            LOG.error("Failed to shutdown", e)
        } catch (e: InternalException) {
            LOG.error("Failed to shutdown", e)
        }
        stack.destroy()
    }
}