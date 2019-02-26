package org.ostelco.ocsgw.datasource.grpc;

import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.cca.ServerCCASession;
import org.ostelco.diameter.CreditControlContext;
import org.ostelco.diameter.model.CreditControlAnswer;
import org.ostelco.diameter.model.CreditControlRequest;
import org.ostelco.diameter.model.MultipleServiceCreditControl;
import org.ostelco.diameter.model.SessionContext;
import org.ostelco.ocs.api.*;
import org.ostelco.ocsgw.OcsServer;
import org.ostelco.ocsgw.datasource.DataSource;
import org.ostelco.ocsgw.metrics.OcsgwMetrics;
import org.ostelco.ocsgw.utils.EventConsumer;
import org.ostelco.ocsgw.utils.EventProducer;
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReport;
import org.ostelco.prime.metrics.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

import static org.ostelco.ocsgw.datasource.grpc.ProtobufToDiameterConverter.convertRequestToGrpc;


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

    private OcsgwMetrics ocsgwAnalytics;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture reconnectStreamFuture = null;

    private Set<String> blocked = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final ConcurrentHashMap<String, CreditControlContext> ccrMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, SessionContext> sessionIdMap = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<CreditControlRequestInfo> requestQueue = new ConcurrentLinkedQueue<>();

    private final EventProducer<CreditControlRequestInfo> producer;

    private Thread consumerThread;


    /**
     * Generate a new instance that connects to an endpoint, and
     * optionally also encrypts the connection.
     *
     * @param ocsServerHostname The gRPC endpoint to connect the client to.
     * @throws IOException
     */
    public GrpcDataSource(final String ocsServerHostname, final String metricsServerHostname) throws IOException {

        this.ocsServerHostname = ocsServerHostname;

        LOG.info("Created GrpcDataSource");
        LOG.info("ocsServerHostname : {}", ocsServerHostname);
        LOG.info("metricsServerHostname : {}", metricsServerHostname);

        // Not using the standard GOOGLE_APPLICATION_CREDENTIALS for this
        // as we need to download the file using container credentials in
        // OcsApplication.
        final String serviceAccountFile = "/config/" + System.getenv("SERVICE_FILE");
        jwtAccessCredentials = ServiceAccountJwtAccessCredentials.fromStream(new FileInputStream(serviceAccountFile));

        ocsgwAnalytics = new OcsgwMetrics(metricsServerHostname, jwtAccessCredentials, this);
        producer = new EventProducer<>(requestQueue);
    }

    @Override
    public void init() {

        setupChannel();
        initCreditControlRequestStream();
        initActivateStream();
        initKeepAlive();
        ocsgwAnalytics.initAnalyticsRequestStream();

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
        final NettyChannelBuilder nettyChannelBuilder = NettyChannelBuilder
                .forTarget(ocsServerHostname);

        try {
            channelBuilder = Files.exists(Paths.get("/cert/ocs.crt"))
                    ? nettyChannelBuilder.sslContext(GrpcSslContexts.forClient().trustManager(new File("/cert/ocs.crt")).build())
                    : nettyChannelBuilder;


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
                    .useTransportSecurity()
                    .keepAliveWithoutCalls(true)
                    .keepAliveTimeout(1, TimeUnit.MINUTES)
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .build();
            ocsServiceStub = OcsServiceGrpc.newStub(grpcChannel).withCallCredentials(MoreCallCredentials.from(jwtAccessCredentials));
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
                        handleGrpcCcrAnswer(answer);
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
     *  Init the gRPC channel that will be used to get activation requests from the
     *  OCS. These requests are send when we need to reactivate a diameter session. For
     *  example on a topup event.
     */
    private void initActivateStream() {
        ActivateRequest dummyActivate = ActivateRequest.newBuilder().build();
        ocsServiceStub.activate(dummyActivate, new StreamObserver<ActivateResponse>() {
            @Override
            public void onNext(ActivateResponse activateResponse) {
                LOG.info("Active user {}", activateResponse.getMsisdn());
                if (sessionIdMap.containsKey(activateResponse.getMsisdn())) {
                    final SessionContext sessionContext = sessionIdMap.get(activateResponse.getMsisdn());
                    OcsServer.getInstance().sendReAuthRequest(sessionContext);
                } else {
                    LOG.debug("No session context stored for msisdn : {}", activateResponse.getMsisdn());
                }
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
                        LOG.debug("Reconnecting GRPC streams");
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


    private void handleGrpcCcrAnswer(CreditControlAnswerInfo answer) {
        try {
            LOG.info("[<<] CreditControlAnswer for {}", answer.getMsisdn());
            final CreditControlContext ccrContext = ccrMap.remove(answer.getRequestId());
            if (ccrContext != null) {
                final ServerCCASession session = OcsServer.getInstance().getStack().getSession(ccrContext.getSessionId(), ServerCCASession.class);
                if (session != null && session.isValid()) {
                    removeFromSessionMap(ccrContext);
                    updateBlockedList(answer, ccrContext.getCreditControlRequest());
                    if (!ccrContext.getSkipAnswer()) {
                        CreditControlAnswer cca = createCreditControlAnswer(answer);
                        try {
                            session.sendCreditControlAnswer(ccrContext.createCCA(cca));
                        } catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException e) {
                            LOG.error("Failed to send Credit-Control-Answer", e);
                        }
                    }
                } else {
                    LOG.warn("No stored CCR or Session for {}", answer.getRequestId());
                }
            } else {
                LOG.warn("Missing CreditControlContext for req id {}", answer.getRequestId());
            }
        } catch (Exception e) {
            LOG.error("Credit-Control-Request failed ", e);
        }
    }

    private void addToSessionMap(CreditControlContext creditControlContext) {
        try {
            SessionContext sessionContext = new SessionContext(creditControlContext.getSessionId(),
                    creditControlContext.getCreditControlRequest().getOriginHost(),
                    creditControlContext.getCreditControlRequest().getOriginRealm(),
                    creditControlContext.getCreditControlRequest().getServiceInformation().get(0).getPsInformation().get(0).getCalledStationId(),
                    creditControlContext.getCreditControlRequest().getServiceInformation().get(0).getPsInformation().get(0).getSgsnMccMnc());
            sessionIdMap.put(creditControlContext.getCreditControlRequest().getMsisdn(), sessionContext);
        } catch (Exception e) {
            LOG.error("Failed to update session map", e);
        }
    }

    private void removeFromSessionMap(CreditControlContext creditControlContext) {
        if (ProtobufToDiameterConverter.getRequestType(creditControlContext) == CreditControlRequestType.TERMINATION_REQUEST) {
            sessionIdMap.remove(creditControlContext.getCreditControlRequest().getMsisdn());
        }
    }

    public OcsgwAnalyticsReport getAnalyticsReport() {
        OcsgwAnalyticsReport.Builder builder = OcsgwAnalyticsReport.newBuilder().setActiveSessions(sessionIdMap.size());
        builder.setKeepAlive(false);
        sessionIdMap.forEach((msisdn, sessionContext) -> {
            builder.addUsers(User.newBuilder().setApn(sessionContext.getApn()).setMccMnc(sessionContext.getMccMnc()).setMsisdn(msisdn).build());
        });
        return builder.build();
    }

    /**
     * A user will be blocked if one of the MSCC in the request could not be filled in the answer
     */
    private void updateBlockedList(CreditControlAnswerInfo answer, CreditControlRequest request) {
        for (org.ostelco.ocs.api.MultipleServiceCreditControl msccAnswer : answer.getMsccList()) {
            for (MultipleServiceCreditControl msccRequest : request.getMultipleServiceCreditControls()) {
                if ((msccAnswer.getServiceIdentifier() == msccRequest.getServiceIdentifier()) && (msccAnswer.getRatingGroup() == msccRequest.getRatingGroup())) {
                    if (updateBlockedList(msccAnswer, msccRequest, answer.getMsisdn())) {
                        return;
                    }
                }
            }
        }
    }

    private boolean updateBlockedList(org.ostelco.ocs.api.MultipleServiceCreditControl msccAnswer, MultipleServiceCreditControl msccRequest, String msisdn) {
        if (!msccRequest.getRequested().isEmpty()) {
            if (msccAnswer.getGranted().getTotalOctets() < msccRequest.getRequested().get(0).getTotal()) {
                blocked.add(msisdn);
                return true;
            } else {
                blocked.remove(msisdn);
            }
        }
        return false;
    }

    @Override
    public void handleRequest(final CreditControlContext context) {

        LOG.info("[>>] creditControlRequest for {}", context.getCreditControlRequest().getMsisdn());

        // FixMe: We should handle conversion errors
        CreditControlRequestInfo creditControlRequestInfo = convertRequestToGrpc(context);
        if (creditControlRequestInfo != null) {
            ccrMap.put(context.getSessionId(), context);
            addToSessionMap(context);
            producer.queueEvent(creditControlRequestInfo);
        }
    }

    private CreditControlAnswer createCreditControlAnswer(CreditControlAnswerInfo response) {
        if (response == null) {
            LOG.error("Empty CreditControlAnswerInfo received");
            return new CreditControlAnswer(org.ostelco.diameter.model.ResultCode.DIAMETER_UNABLE_TO_COMPLY, new ArrayList<>());
        }

        final LinkedList<MultipleServiceCreditControl> multipleServiceCreditControls = new LinkedList<>();
        for (org.ostelco.ocs.api.MultipleServiceCreditControl mscc : response.getMsccList()) {
            multipleServiceCreditControls.add(ProtobufToDiameterConverter.convertMSCC(mscc));
        }
        return new CreditControlAnswer(ProtobufToDiameterConverter.convertResultCode(response.getResultCode()), multipleServiceCreditControls);
    }


    @Override
    public boolean isBlocked(final String msisdn) {
        return blocked.contains(msisdn);
    }
}
