package org.ostelco.ocsgw.datasource.grpc

import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials
import io.grpc.StatusRuntimeException
import io.grpc.auth.MoreCallCredentials
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import org.jdiameter.api.IllegalDiameterStateException
import org.jdiameter.api.InternalException
import org.jdiameter.api.OverloadException
import org.jdiameter.api.RouteException
import org.jdiameter.api.cca.ServerCCASession
import org.ostelco.diameter.CreditControlContext
import org.ostelco.diameter.model.CreditControlAnswer
import org.ostelco.diameter.model.CreditControlRequest
import org.ostelco.diameter.model.MultipleServiceCreditControl
import org.ostelco.diameter.model.SessionContext
import org.ostelco.ocs.api.*
import org.ostelco.ocsgw.OcsServer
import org.ostelco.ocsgw.datasource.DataSource
import org.ostelco.ocsgw.datasource.grpc.GrpcDiameterConverter.convertRequestToGrpc
import org.ostelco.ocsgw.metrics.OcsgwMetrics
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReport
import org.ostelco.prime.metrics.api.User
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.*

/**
 * Uses gRPC to fetch data remotely
 */
class GrpcDataSource
/**
 * Generate a new instance that connects to an endpoint, and
 * optionally also encrypts the connection.
 *
 * @param ocsServerHostname The gRPC endpoint to connect the client to.
 * @throws IOException
 */
@Throws(IOException::class)
constructor(ocsServerHostname: String, metricsServerHostname: String) : DataSource {

    private val ocsServiceStub: OcsServiceGrpc.OcsServiceStub

    private var creditControlRequest: StreamObserver<CreditControlRequestInfo>? = null

    private val ocsgwAnalytics: OcsgwMetrics

    private val executorService = Executors.newSingleThreadScheduledExecutor()

    private var keepAliveFuture: ScheduledFuture<*>? = null

    private var initActivateFuture: ScheduledFuture<*>? = null

    private var initCCRFuture: ScheduledFuture<*>? = null

    private val blocked = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private val ccrMap = ConcurrentHashMap<String, CreditControlContext>()

    private val sessionIdMap = ConcurrentHashMap<String, SessionContext>()


    val analyticsReport: OcsgwAnalyticsReport
        get() {
            LOG.info("Number of active sessions is {}", sessionIdMap.size)
            val builder = OcsgwAnalyticsReport.newBuilder().setActiveSessions(sessionIdMap.size)
            builder.keepAlive = false
            sessionIdMap.forEach { msisdn, (_, _, _, apn, mccMnc) -> builder.addUsers(User.newBuilder().setApn(apn).setMccMnc(mccMnc).setMsisdn(msisdn).build()) }
            return builder.build()
        }


    init {
        LOG.info("Created GrpcDataSource")
        LOG.info("ocsServerHostname : {}", ocsServerHostname)
        LOG.info("metricsServerHostname : {}", metricsServerHostname)

        // Set up a channel to be used to communicate as an OCS instance,
        // to a gRPC instance.
        val nettyChannelBuilder = NettyChannelBuilder
                .forTarget(ocsServerHostname)
                .keepAliveWithoutCalls(true)
                .keepAliveTimeout(1, TimeUnit.MINUTES)
                .keepAliveTime(50, TimeUnit.SECONDS)

        val channelBuilder = if (Files.exists(Paths.get("/cert/ocs.crt")))
            nettyChannelBuilder.sslContext(GrpcSslContexts.forClient().trustManager(File("/cert/ocs.crt")).build())
        else
            nettyChannelBuilder

        // Not using the standard GOOGLE_APPLICATION_CREDENTIALS for this
        // as we need to download the file using container credentials in
        // OcsApplication.
        val serviceAccountFile = "/config/" + System.getenv("SERVICE_FILE")
        val credentials = ServiceAccountJwtAccessCredentials.fromStream(FileInputStream(serviceAccountFile))
        val channel = channelBuilder
                .usePlaintext()
                .build()
        ocsServiceStub = OcsServiceGrpc.newStub(channel)
                .withCallCredentials(MoreCallCredentials.from(credentials))

        ocsgwAnalytics = OcsgwMetrics(metricsServerHostname, credentials, this)
    }

    override fun init() {

        initCreditControlRequest()
        initActivate()
        initKeepAlive()
        ocsgwAnalytics.initAnalyticsRequest()
    }


    /**
     * Init the gRPC channel that will be used to send/receive
     * diameter messages to the OCS module in Prime.
     */
    private fun initCreditControlRequest() {
        creditControlRequest = ocsServiceStub.creditControlRequest(
                object : StreamObserver<CreditControlAnswerInfo> {
                    override fun onNext(answer: CreditControlAnswerInfo) {
                        CoroutineScope(Dispatchers.Default).launch {
                            handleGrpcCcrAnswer(answer)
                        }
                    }

                    override fun onError(t: Throwable) {
                        LOG.error("CreditControlRequestObserver error {} {}", t.message, t)
                        if (t is StatusRuntimeException) {
                            reconnectCreditControlRequest()
                        }
                    }

                    override fun onCompleted() {
                        // Nothing to do here
                    }
                })
    }

    /**
     * Init the gRPC channel that will be used to get activation requests from the
     * OCS. These requests are send when we need to reactivate a diameter session. For
     * example on a topup event.
     */
    private fun initActivate() {
        val dummyActivate = ActivateRequest.newBuilder().build()
        ocsServiceStub.activate(dummyActivate, object : StreamObserver<ActivateResponse> {
            override fun onNext(activateResponse: ActivateResponse) {
                LOG.info("Active user {}", activateResponse.msisdn)
                if (sessionIdMap.containsKey(activateResponse.msisdn)) {
                    val sessionContext = sessionIdMap[activateResponse.msisdn]
                    OcsServer.getInstance().sendReAuthRequest(sessionContext)
                } else {
                    LOG.debug("No session context stored for msisdn : {}", activateResponse.msisdn)
                }
            }

            override fun onError(t: Throwable) {
                LOG.error("ActivateObserver error", t)
                if (t is StatusRuntimeException) {
                    reconnectActivate()
                }
            }

            override fun onCompleted() {
                // Nothing to do here
            }
        })
    }

    private fun sendKeepAlive() {
            val ccr = CreditControlRequestInfo.newBuilder()
                    .setType(CreditControlRequestType.NONE)
                    .build()
            sendRequest(ccr)
    }

    /**
     * The keep alive messages are sent on the  CreditControlRequest stream
     * to force it to stay open avoiding reconnects on the gRPC channel.
     */
    private fun initKeepAlive() {
        // this is used to keep connection alive
        keepAliveFuture = executorService.scheduleWithFixedDelay({
            sendKeepAlive()
        },
                15,
                50,
                TimeUnit.SECONDS)
    }

    private fun reconnectActivate() {
        LOG.debug("reconnectActivate called")

        if (initActivateFuture != null) {
            initActivateFuture!!.cancel(true)
        }

        LOG.debug("Schedule new Callable initActivate")
        initActivateFuture = executorService.schedule({
            LOG.debug("Calling initActivate")
            initActivate()
            "Called!"
        } as Callable<Object>,
                5,
                TimeUnit.SECONDS)
    }

    private fun reconnectCcrKeepAlive() {
        LOG.debug("reconnectCcrKeepAlive called")
        if (keepAliveFuture != null) {
            keepAliveFuture!!.cancel(true)
        }

        initKeepAlive()
    }


    private fun reconnectCreditControlRequest() {
        LOG.debug("reconnectCreditControlRequest called")

        if (initCCRFuture != null) {
            initCCRFuture!!.cancel(true)
        }

        LOG.debug("Schedule new Callable initCreditControlRequest")
        initCCRFuture = executorService.schedule({
            reconnectCcrKeepAlive()
            LOG.debug("Calling initCreditControlRequest")
            initCreditControlRequest()
            "Called!"
        } as Callable<Object>,
                5,
                TimeUnit.SECONDS)
    }


    private suspend fun handleGrpcCcrAnswer(answer: CreditControlAnswerInfo) {
        try {
            LOG.info("[<<] CreditControlAnswer for {}", answer.msisdn)
            val ccrContext = ccrMap.remove(answer.requestId)
            if (ccrContext != null) {
                val session = OcsServer.getInstance().stack.getSession<ServerCCASession>(ccrContext.sessionId, ServerCCASession::class.java)
                if (session != null && session.isValid) {
                    removeFromSessionMap(ccrContext)
                    updateBlockedList(answer, ccrContext.creditControlRequest)
                    if (!ccrContext.skipAnswer) {
                        val cca = createCreditControlAnswer(answer)
                        withContext(Dispatchers.Default) {
                            try {
                                session.sendCreditControlAnswer(ccrContext.createCCA(cca))
                            } catch (e: InternalException) {
                                LOG.error("Failed to send Credit-Control-Answer", e)
                            } catch (e: IllegalDiameterStateException) {
                                LOG.error("Failed to send Credit-Control-Answer", e)
                            } catch (e: RouteException) {
                                LOG.error("Failed to send Credit-Control-Answer", e)
                            } catch (e: OverloadException) {
                                LOG.error("Failed to send Credit-Control-Answer", e)
                            }
                        }
                    }
                } else {
                    LOG.warn("No stored CCR or Session for {}", answer.requestId)
                }
            } else {
                LOG.warn("Missing CreditControlContext for req id {}", answer.requestId)
            }
        } catch (e: Exception) {
            LOG.error("Credit-Control-Request failed ", e)
        }

    }

    private fun addToSessionMap(creditControlContext: CreditControlContext) {
        try {
            val sessionContext = SessionContext(creditControlContext.sessionId,
                    creditControlContext.creditControlRequest.originHost,
                    creditControlContext.creditControlRequest.originRealm,
                    creditControlContext.creditControlRequest.serviceInformation[0].psInformation[0].calledStationId,
                    creditControlContext.creditControlRequest.serviceInformation[0].psInformation[0].sgsnMccMnc)
            sessionIdMap[creditControlContext.creditControlRequest.msisdn] = sessionContext
        } catch (e: Exception) {
            LOG.error("Failed to update session map", e)
        }

    }

    private fun removeFromSessionMap(creditControlContext: CreditControlContext) {
        if (GrpcDiameterConverter.getRequestType(creditControlContext) == CreditControlRequestType.TERMINATION_REQUEST) {
            sessionIdMap.remove(creditControlContext.creditControlRequest.msisdn)
        }
    }

    /**
     * A user will be blocked if one of the MSCC in the request could not be filled in the answer
     */
    private fun updateBlockedList(answer: CreditControlAnswerInfo, request: CreditControlRequest) {
        for (msccAnswer in answer.msccList) {
            for (msccRequest in request.multipleServiceCreditControls) {
                if (msccAnswer.serviceIdentifier == msccRequest.serviceIdentifier && msccAnswer.ratingGroup == msccRequest.ratingGroup) {
                    if (updateBlockedList(msccAnswer, msccRequest, answer.msisdn)) {
                        return
                    }
                }
            }
        }
    }

    private fun updateBlockedList(msccAnswer: org.ostelco.ocs.api.MultipleServiceCreditControl, msccRequest: MultipleServiceCreditControl, msisdn: String): Boolean {
        if (!msccRequest.requested.isEmpty()) {
            if (msccAnswer.granted.totalOctets < msccRequest.requested[0].total) {
                blocked.add(msisdn)
                return true
            } else {
                blocked.remove(msisdn)
            }
        }
        return false
    }

    override fun handleRequest(context: CreditControlContext) {
        CoroutineScope(Dispatchers.Default).launch {

            LOG.info("[>>] creditControlRequest for {}", context.creditControlRequest.msisdn)

            // FixMe: We should handle conversion errors
            val creditControlRequestInfo = convertRequestToGrpc(context)
            if (creditControlRequestInfo != null) {
                ccrMap[context.sessionId] = context
                addToSessionMap(context)
                withContext(Dispatchers.Default) {
                    sendRequest(creditControlRequestInfo)
                }
            }
        }
    }

    private fun createCreditControlAnswer(response: CreditControlAnswerInfo?): CreditControlAnswer {
        if (response == null) {
            LOG.error("Empty CreditControlAnswerInfo received")
            return CreditControlAnswer(org.ostelco.diameter.model.ResultCode.DIAMETER_UNABLE_TO_COMPLY, ArrayList())
        }

        val multipleServiceCreditControls = LinkedList<MultipleServiceCreditControl>()
        for (mscc in response.msccList) {
            multipleServiceCreditControls.add(GrpcDiameterConverter.convertMSCC(mscc))
        }
        return CreditControlAnswer(GrpcDiameterConverter.convertResultCode(response.resultCode), multipleServiceCreditControls)
    }


    override fun isBlocked(msisdn: String): Boolean {
        return blocked.contains(msisdn)
    }

    fun sendRequest(requestInfo: CreditControlRequestInfo) {

        if (requestInfo.requestId.isEmpty()) {
            LOG.warn("Empty requestId in requestInfo {}", requestInfo)
        }

        synchronized(GrpcDataSource) {
            creditControlRequest!!.onNext(requestInfo)
        }
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(GrpcDataSource::class.java)
    }
}
