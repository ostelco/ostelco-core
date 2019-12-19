package org.ostelco.ocsgw.datasource.protobuf

import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import io.grpc.auth.MoreCallCredentials
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.StreamObserver
import org.ostelco.diameter.CreditControlContext
import org.ostelco.diameter.getLogger
import org.ostelco.ocs.api.*
import org.ostelco.ocs.api.OcsServiceGrpc.OcsServiceStub
import org.ostelco.ocsgw.datasource.DataSource
import org.ostelco.ocsgw.utils.EventConsumer
import org.ostelco.ocsgw.utils.EventProducer
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.*
import javax.net.ssl.SSLException

/**
 * Uses gRPC to fetch data remotely
 */
class GrpcDataSource(
        private val protobufDataSource: ProtobufDataSource,
        private val ocsServerHostname: String) : DataSource {

    private var ocsServiceStub: OcsServiceStub? = null
    private lateinit var creditControlRequestStream: StreamObserver<CreditControlRequestInfo>
    private var grpcChannel: ManagedChannel? = null
    private val jwtAccessCredentials: ServiceAccountJwtAccessCredentials
    private val executorService = Executors.newSingleThreadScheduledExecutor()
    private var reconnectStreamFuture: ScheduledFuture<*>? = null
    private val requestQueue = ConcurrentLinkedQueue<CreditControlRequestInfo>()
    private val producer: EventProducer<CreditControlRequestInfo>
    private var consumerThread: Thread? = null
    private val logger by getLogger()

    override fun init() {
        setupChannel()
        initCreditControlRequestStream()
        initActivateStream()
        initKeepAlive()
        setupEventConsumer()
    }

    private fun setupEventConsumer() {

        consumerThread?.interrupt()

        val requestInfoConsumer = EventConsumer(requestQueue, creditControlRequestStream)
        consumerThread = Thread(requestInfoConsumer)
        consumerThread!!.start()
    }

    private fun setupChannel() {
        val channelBuilder: ManagedChannelBuilder<*>
        // Set up a channel to be used to communicate as an OCS instance,
        // to a gRPC instance.
        val disableTls = java.lang.Boolean.valueOf(System.getenv("DISABLE_TLS"))
        try {
            channelBuilder = if (disableTls) {
                ManagedChannelBuilder
                        .forTarget(ocsServerHostname)
                        .usePlaintext()
            } else {
                val nettyChannelBuilder = NettyChannelBuilder.forTarget(ocsServerHostname)
                if (Files.exists(Paths.get("/cert/ocs.crt")))
                    nettyChannelBuilder.sslContext(
                            GrpcSslContexts.forClient().trustManager(File("/cert/ocs.crt")).build())
                            .useTransportSecurity()
                else
                    nettyChannelBuilder.useTransportSecurity()
            }

            grpcChannel?.let{shutdownGprsChannel(it)}

            grpcChannel = channelBuilder
                    .keepAliveWithoutCalls(true)
                    .build()
            ocsServiceStub = OcsServiceGrpc.newStub(grpcChannel)
                    .withCallCredentials(MoreCallCredentials.from(jwtAccessCredentials))
        } catch (e: SSLException) {
            logger.warn("Failed to setup gRPC channel", e)
        }
    }

    private fun shutdownGprsChannel(grpcChannel: ManagedChannel) {
        grpcChannel.shutdownNow()
        try {
            val isShutdown = grpcChannel.awaitTermination(3, TimeUnit.SECONDS)
            logger.info("grpcChannel is shutdown : $isShutdown")
        } catch (e: InterruptedException) {
            logger.info("Error shutting down gRPC channel", e)
        }
    }

    /**
     * Init the gRPC channel that will be used to send/receive
     * Diameter messages to the OCS module in Prime.
     */
    private fun initCreditControlRequestStream() {
        creditControlRequestStream = ocsServiceStub!!.creditControlRequest(
                object : StreamObserver<CreditControlAnswerInfo?> {
                    override fun onNext(answer: CreditControlAnswerInfo?) {
                        protobufDataSource.handleCcrAnswer(answer!!)
                    }

                    override fun onError(t: Throwable) {
                        logger.error("CreditControlRequestStream error", t)
                        if (t is StatusRuntimeException) {
                            reconnectStreams()
                        }
                    }

                    override fun onCompleted() { // Nothing to do here
                    }
                })
    }

    /**
     * Init the gRPC channel that will be used to get activation requests from the
     * OCS. These requests are send when we need to reactivate a diameter session. For
     * example on a topup event.
     */
    private fun initActivateStream() {
        val dummyActivate = ActivateRequest.newBuilder().build()
        ocsServiceStub!!.activate(dummyActivate, object : StreamObserver<ActivateResponse?> {
            override fun onNext(activateResponse: ActivateResponse?) {
                protobufDataSource.handleActivateResponse(activateResponse!!)
            }

            override fun onError(t: Throwable) {
                logger.error("ActivateObserver error", t)
                if (t is StatusRuntimeException) {
                    reconnectStreams()
                }
            }

            override fun onCompleted() { // Nothing to do here
            }
        })
    }

    /**
     * The keep alive messages are sent on the creditControlRequestStream
     * to force it to stay open, avoiding reconnects on the gRPC channel.
     */
    private fun initKeepAlive() {
        executorService.scheduleWithFixedDelay({
            val ccr = CreditControlRequestInfo.newBuilder()
                    .setType(CreditControlRequestType.NONE)
                    .build()
            producer.queueEvent(ccr)
        },
                10,
                5,
                TimeUnit.SECONDS)
    }

    private fun reconnectStreams() {
        logger.debug("reconnectStreams called")
        if (!isReconnecting) {
            reconnectStreamFuture = executorService.schedule(Callable<Any> {
                logger.debug("Reconnecting gRPC streams")
                setupChannel()
                initCreditControlRequestStream()
                setupEventConsumer()
                initActivateStream()
                "Called!"
            },
                    5,
                    TimeUnit.SECONDS)
        }
    }

    private val isReconnecting: Boolean
        get() = if (reconnectStreamFuture != null) {
            !reconnectStreamFuture!!.isDone
        } else false

    override fun handleRequest(context: CreditControlContext) {
        val creditControlRequestInfo = protobufDataSource.handleRequest(context, null)
        if (creditControlRequestInfo != null) {
            context.sentToOcsTime = System.currentTimeMillis()
            producer.queueEvent(creditControlRequestInfo)
        } else {
            logger.error("Failed to handle request ${context.sessionId}")
        }
    }

    override fun isBlocked(msisdn: String): Boolean {
        return protobufDataSource.isBlocked(msisdn)
    }

    /**
     * Generate a new instance that connects to an endpoint, and
     * optionally also encrypts the connection.
     *
     * @param ocsServerHostname The gRPC endpoint to connect the client to.
     * @throws IOException
     */
    init {
        logger.info("Created GrpcDataSource")
        logger.info("ocsServerHostname : {}", ocsServerHostname)
        /**
         * Not using the standard GOOGLE_APPLICATION_CREDENTIALS for this
         * as we need to download the file using container credentials in
         * OcsApplication.
         **/
        val serviceAccountFile = "/config/" + System.getenv("SERVICE_FILE")
        jwtAccessCredentials = ServiceAccountJwtAccessCredentials.fromStream(FileInputStream(serviceAccountFile))
        producer = EventProducer(requestQueue)
    }
}