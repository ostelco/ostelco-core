package org.ostelco.ocsgw.datasource.protobuf;

import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.ostelco.diameter.CreditControlContext;
import org.ostelco.ocs.api.ActivateRequest;
import org.ostelco.ocs.api.ActivateResponse;
import org.ostelco.ocs.api.CreditControlAnswerInfo;
import org.ostelco.ocs.api.CreditControlRequestInfo;
import org.ostelco.ocs.api.CreditControlRequestType;
import org.ostelco.ocs.api.OcsServiceGrpc;
import org.ostelco.ocsgw.datasource.DataSource;
import org.ostelco.ocsgw.utils.EventConsumer;
import org.ostelco.ocsgw.utils.EventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Uses gRPC to fetch data remotely
 */
public class GrpcDataSource implements DataSource {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcDataSource.class);

    private OcsServiceGrpc.OcsServiceStub ocsServiceStub;

    private StreamObserver<CreditControlRequestInfo> creditControlRequestStream;

    private String ocsServerHostname;

    private ManagedChannel grpcChannel;

    private ServiceAccountJwtAccessCredentials jwtAccessCredentials;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture reconnectStreamFuture = null;

    private final ConcurrentLinkedQueue<CreditControlRequestInfo> requestQueue = new ConcurrentLinkedQueue<>();

    private final EventProducer<CreditControlRequestInfo> producer;

    private Thread consumerThread;

    private ProtobufDataSource protobufDataSource;

    /**
     * Generate a new instance that connects to an endpoint, and
     * optionally also encrypts the connection.
     *
     * @param ocsServerHostname The gRPC endpoint to connect the client to.
     * @throws IOException
     */
    public GrpcDataSource(
            final ProtobufDataSource protobufDataSource,
            final String ocsServerHostname) throws IOException {

        this.protobufDataSource = protobufDataSource;

        this.ocsServerHostname = ocsServerHostname;

        LOG.info("Created GrpcDataSource");
        LOG.info("ocsServerHostname : {}", ocsServerHostname);

        // Not using the standard GOOGLE_APPLICATION_CREDENTIALS for this
        // as we need to download the file using container credentials in
        // OcsApplication.
        final String serviceAccountFile = "/config/" + System.getenv("SERVICE_FILE");
        jwtAccessCredentials = ServiceAccountJwtAccessCredentials.fromStream(new FileInputStream(serviceAccountFile));

        producer = new EventProducer<>(requestQueue);
    }

    @Override
    public void init() {

        setupChannel();
        initCreditControlRequestStream();
        initActivateStream();
        initKeepAlive();

        setupEventConsumer();
    }

    private void setupEventConsumer() {

        // ToDo : Is this enough to know the thread stopped?
        if (consumerThread != null) {
            consumerThread.interrupt();
        }

        EventConsumer<CreditControlRequestInfo> requestInfoConsumer = new EventConsumer<>(requestQueue, creditControlRequestStream);
        consumerThread = new Thread(requestInfoConsumer);
        consumerThread.start();
    }

    private void setupChannel() {


        ManagedChannelBuilder channelBuilder;

        // Set up a channel to be used to communicate as an OCS instance,
        // to a gRPC instance.

        final boolean disableTls = Boolean.valueOf(System.getenv("DISABLE_TLS"));

        try {
            if (disableTls) {
                channelBuilder = ManagedChannelBuilder
                        .forTarget(ocsServerHostname)
                        .usePlaintext();
            } else {
                final NettyChannelBuilder nettyChannelBuilder = NettyChannelBuilder
                        .forTarget(ocsServerHostname);


                channelBuilder = Files.exists(Paths.get("/cert/ocs.crt"))
                        ? nettyChannelBuilder.sslContext(
                        GrpcSslContexts.forClient().trustManager(new File("/cert/ocs.crt")).build())
                        .useTransportSecurity()
                        : nettyChannelBuilder.useTransportSecurity();
            }

            if (grpcChannel != null) {

                grpcChannel.shutdownNow();
                try {
                    boolean isShutdown = grpcChannel.awaitTermination(3, TimeUnit.SECONDS);
                    LOG.info("grpcChannel is shutdown : " + isShutdown);
                } catch (InterruptedException e) {
                    LOG.info("Error shutting down gRPC channel");
                }
            }

            grpcChannel = channelBuilder
                    .keepAliveWithoutCalls(true)
//                    .keepAliveTimeout(1, TimeUnit.MINUTES)
//                    .keepAliveTime(20, TimeUnit.MINUTES)
                    .build();

            ocsServiceStub = OcsServiceGrpc.newStub(grpcChannel)
                    .withCallCredentials(MoreCallCredentials.from(jwtAccessCredentials));

        } catch (SSLException e) {
            LOG.warn("Failed to setup gRPC channel", e);
        }
    }


    /**
     * Init the gRPC channel that will be used to send/receive
     * diameter messages to the OCS module in Prime.
     */
    private void initCreditControlRequestStream() {
        creditControlRequestStream = ocsServiceStub.creditControlRequest(
                new StreamObserver<CreditControlAnswerInfo>() {
                    public void onNext(CreditControlAnswerInfo answer) {
                        protobufDataSource.handleCcrAnswer(answer);
                    }

                    @Override
                    public void onError(Throwable t) {
                        LOG.error("CreditControlRequestStream error", t);
                        if (t instanceof StatusRuntimeException) {
                            reconnectStreams();
                        }
                    }

                    @Override
                    public void onCompleted() {
                        // Nothing to do here
                    }
                });
    }

    /**
     * Init the gRPC channel that will be used to get activation requests from the
     * OCS. These requests are send when we need to reactivate a diameter session. For
     * example on a topup event.
     */
    private void initActivateStream() {
        ActivateRequest dummyActivate = ActivateRequest.newBuilder().build();
        ocsServiceStub.activate(dummyActivate, new StreamObserver<ActivateResponse>() {
            @Override
            public void onNext(ActivateResponse activateResponse) {
                protobufDataSource.handleActivateResponse(activateResponse);
            }

            @Override
            public void onError(Throwable t) {
                LOG.error("ActivateObserver error", t);
                if (t instanceof StatusRuntimeException) {
                    reconnectStreams();
                }
            }

            @Override
            public void onCompleted() {
                // Nothing to do here
            }
        });
    }

    /**
     * The keep alive messages are sent on the creditControlRequestStream
     * to force it to stay open avoiding reconnects on the gRPC channel.
     */
    private void initKeepAlive() {
        // this is used to keep connection alive
        executorService.scheduleWithFixedDelay(() -> {
                    final CreditControlRequestInfo ccr = CreditControlRequestInfo.newBuilder()
                            .setType(CreditControlRequestType.NONE)
                            .build();
                    producer.queueEvent(ccr);
                },
                10,
                5,
                TimeUnit.SECONDS);
    }


    private void reconnectStreams() {
        LOG.debug("reconnectStreams called");

        if (!isReconnecting()) {

            reconnectStreamFuture = executorService.schedule((Callable<Object>) () -> {
                        LOG.debug("Reconnecting gRPC streams");
                        setupChannel();
                        initCreditControlRequestStream();
                        setupEventConsumer();
                        initActivateStream();
                        return "Called!";
                    },
                    5,
                    TimeUnit.SECONDS);
        }
    }

    private boolean isReconnecting() {
        if (reconnectStreamFuture != null) {
            return !reconnectStreamFuture.isDone();
        }
        return false;
    }

    @Override
    public void handleRequest(final CreditControlContext context) {

        CreditControlRequestInfo creditControlRequestInfo = protobufDataSource.handleRequest(context, null);

        if (creditControlRequestInfo != null) {
            producer.queueEvent(creditControlRequestInfo);
        }
    }

    @Override
    public boolean isBlocked(final String msisdn) {
        return protobufDataSource.isBlocked(msisdn);
    }
}
