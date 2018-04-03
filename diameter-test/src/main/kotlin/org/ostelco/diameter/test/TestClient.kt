package org.ostelco.diameter.test

import org.jdiameter.api.Answer
import org.jdiameter.api.ApplicationId
import org.jdiameter.api.Avp
import org.jdiameter.api.AvpSet
import org.jdiameter.api.Configuration
import org.jdiameter.api.EventListener
import org.jdiameter.api.IllegalDiameterStateException
import org.jdiameter.api.InternalException
import org.jdiameter.api.Message
import org.jdiameter.api.Network
import org.jdiameter.api.NetworkReqListener
import org.jdiameter.api.OverloadException
import org.jdiameter.api.Request
import org.jdiameter.api.RouteException
import org.jdiameter.api.Session
import org.jdiameter.api.SessionFactory
import org.jdiameter.api.Stack
import org.jdiameter.common.impl.app.cca.JCreditControlRequestImpl
import org.jdiameter.server.impl.StackImpl
import org.jdiameter.server.impl.helpers.XMLConfiguration
import org.ostelco.diameter.logger
import org.ostelco.diameter.util.DiameterUtilities
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val ec = TestClient()
    ec.initStack("src/main/resources/")
    ec.start()

    while (ec.isAnswerReceived) {
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }
}

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

    //stack and session factory
    private lateinit var stack: Stack

    private lateinit var factory: SessionFactory

    // session used as handle for communication
    var session: Session? = null
        private set

    //boolean telling if we received an answer
    var isAnswerReceived = false
        private set

    //boolean telling if we received an answer
    var isRequestReceived = false
        private set

    //Parse stack configuration
    private lateinit var config: Configuration

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
            stack.start()
            LOG.info("Stack is running.")
        } catch (e: Exception) {
            LOG.error("Failed to start Diameter Stack", e)
            stack.destroy()
            return
        }

        LOG.info("Stack initialization successfully completed.")
    }

    private fun printApplicationInfo() {
        //Print info about application
        val appIds = stack.metaData.localPeer.commonApplications

        LOG.info("Diameter Stack  :: Supporting " + appIds.size + " applications.")
        for (id in appIds) {
            LOG.info("Diameter Stack  :: Common :: $id")
        }
    }

    fun initRequestTest() {
        this.isRequestReceived = false
    }

    fun createRequest(realm : String, host : String): Request? {
        return session?.createRequest(
                commandCode,
                ApplicationId.createByAuthAppId(applicationID),
                realm,
                host
        );
    }

    fun start() {
        try {
            //wait for connection to peer
            Thread.sleep(5000)
            this.session = this.factory.getNewSession("BadCustomSessionId;" + System.currentTimeMillis() + ";0")
        } catch (e: InternalException) {
            LOG.error("Start Failed", e)
        } catch (e: InterruptedException) {
            LOG.error("Start Failed", e)
        }

    }

    fun sendNextRequest(request: Request) {
        val ccr = JCreditControlRequestImpl(request)
        isAnswerReceived = false
        try {
            this.session!!.send(ccr.message, this)
            dumpMessage(ccr.message, true) //dump info on console
        } catch (e: InternalException) {
            LOG.error("Failed to send request", e)
            isAnswerReceived = true
        } catch (e: IllegalDiameterStateException) {
            LOG.error("Failed to send request", e)
            isAnswerReceived = true
        } catch (e: RouteException) {
            LOG.error("Failed to send request", e)
            isAnswerReceived = true
        } catch (e: OverloadException) {
            LOG.error("Failed to send request", e)
            isAnswerReceived = true
        }
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