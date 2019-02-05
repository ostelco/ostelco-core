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
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReport;
import org.ostelco.prime.metrics.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

import static org.ostelco.ocsgw.datasource.grpc.GrpcDiameterConverter.convertRequestToGrpc;


/**
 * Uses gRPC to fetch data remotely
 */
public class GrpcDataSource implements DataSource {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcDataSource.class);

    private final OcsServiceGrpc.OcsServiceStub ocsServiceStub;

    private StreamObserver<CreditControlRequestInfo> creditControlRequest;

    private OcsgwMetrics ocsgwAnalytics;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture keepAliveFuture = null;

    private ScheduledFuture initActivateFuture = null;

    private ScheduledFuture initCCRFuture = null;

    private static final int MAX_ENTRIES = 50000;

    Set<String> blocked = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final ConcurrentHashMap<String, CreditControlContext> ccrMap = new ConcurrentHashMap<>(MAX_ENTRIES, .75F);

    private final ConcurrentHashMap<String, SessionContext> sessionIdMap = new ConcurrentHashMap<>(MAX_ENTRIES, .75F);


    /**
     * Generate a new instance that connects to an endpoint, and
     * optionally also encrypts the connection.
     *
     * @param ocsServerHostname The gRPC endpoint to connect the client to.
     * @throws IOException
     */
    public GrpcDataSource(final String ocsServerHostname, final String metricsServerHostname) throws IOException {

        LOG.info("Created GrpcDataSource");
        LOG.info("ocsServerHostname : {}", ocsServerHostname);
        LOG.info("metricsServerHostname : {}", metricsServerHostname);

        // Set up a channel to be used to communicate as an OCS instance,
        // to a gRPC instance.
        final NettyChannelBuilder nettyChannelBuilder = NettyChannelBuilder
                .forTarget(ocsServerHostname)
                .keepAliveWithoutCalls(true)
                .keepAliveTimeout(1, TimeUnit.MINUTES)
                .keepAliveTime(50, TimeUnit.SECONDS);

        final ManagedChannelBuilder channelBuilder =
                Files.exists(Paths.get("/cert/ocs.crt"))
                        ? nettyChannelBuilder.sslContext(GrpcSslContexts.forClient().trustManager(new File("/cert/ocs.crt")).build())
                        : nettyChannelBuilder;

        // Not using the standard GOOGLE_APPLICATION_CREDENTIALS for this
        // as we need to download the file using container credentials in
        // OcsApplication.
        final String serviceAccountFile = "/config/" + System.getenv("SERVICE_FILE");
        final ServiceAccountJwtAccessCredentials credentials =
                ServiceAccountJwtAccessCredentials.fromStream(new FileInputStream(serviceAccountFile));
        final ManagedChannel channel = channelBuilder
                .useTransportSecurity()
                .build();
        ocsServiceStub = OcsServiceGrpc.newStub(channel)
                .withCallCredentials(MoreCallCredentials.from(credentials));

        ocsgwAnalytics = new OcsgwMetrics(metricsServerHostname, credentials);
    }

    @Override
    public void init() {

        initCreditControlRequest();
        initActivate();
        initKeepAlive();
        ocsgwAnalytics.initAnalyticsRequest();
    }


    /**
     * Init the gRPC channel that will be used to send/receive
     * diameter messages to the OCS module in Prime.
     */
    private void initCreditControlRequest() {
        creditControlRequest = ocsServiceStub.creditControlRequest(
                new StreamObserver<CreditControlAnswerInfo>() {
                    public void onNext(CreditControlAnswerInfo answer) {
                        handleGrpcCcrAnswer(answer);
                    }

                    @Override
                    public void onError(Throwable t) {
                        LOG.error("CreditControlRequestObserver error", t);
                        if (t instanceof StatusRuntimeException) {
                            reconnectCreditControlRequest();
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
    private void initActivate() {
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
                    reconnectActivate();
                }
            }

            @Override
            public void onCompleted() {
                // Nothing to do here
            }
        });
    }

    /**
     * The keep alive messages are sent on the  CreditControlRequest stream
     * to force it to stay open avoiding reconnects on the gRPC channel.
     */
    private void initKeepAlive() {
        // this is used to keep connection alive
        keepAliveFuture = executorService.scheduleWithFixedDelay(() -> {
                    final CreditControlRequestInfo ccr = CreditControlRequestInfo.newBuilder()
                            .setType(CreditControlRequestType.NONE)
                            .build();
                    creditControlRequest.onNext(ccr);
                },
                15,
                50,
                TimeUnit.SECONDS);
    }

    private void reconnectActivate() {
        LOG.debug("reconnectActivate called");

        if (initActivateFuture != null) {
            initActivateFuture.cancel(true);
        }

        LOG.debug("Schedule new Callable initActivate");
        initActivateFuture = executorService.schedule((Callable<Object>) () -> {
                    LOG.debug("Calling initActivate");
                    initActivate();
                    return "Called!";
                },
                5,
                TimeUnit.SECONDS);
    }

    private void reconnectCcrKeepAlive() {
        LOG.debug("reconnectCcrKeepAlive called");
        if (keepAliveFuture != null) {
            keepAliveFuture.cancel(true);
        }

        initKeepAlive();
    }


    private void reconnectCreditControlRequest() {
        LOG.debug("reconnectCreditControlRequest called");

        if (initCCRFuture != null) {
            initCCRFuture.cancel(true);
        }

        LOG.debug("Schedule new Callable initCreditControlRequest");
        initCCRFuture = executorService.schedule((Callable<Object>) () -> {
                    reconnectCcrKeepAlive();
                    LOG.debug("Calling initCreditControlRequest");
                    initCreditControlRequest();
                    return "Called!";
                },
                5,
                TimeUnit.SECONDS);
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
            if (sessionIdMap.put(creditControlContext.getCreditControlRequest().getMsisdn(), sessionContext) == null) {
                updateAnalytics();
            }
        } catch (Exception e) {
            LOG.error("Failed to update session map", e);
        }
    }

    private void removeFromSessionMap(CreditControlContext creditControlContext) {
        if (GrpcDiameterConverter.getRequestType(creditControlContext) == CreditControlRequestType.TERMINATION_REQUEST) {
            sessionIdMap.remove(creditControlContext.getCreditControlRequest().getMsisdn());
            updateAnalytics();
        }
    }

    private void updateAnalytics() {
        LOG.info("Number of active sessions is {}", sessionIdMap.size());

        OcsgwAnalyticsReport.Builder builder = OcsgwAnalyticsReport.newBuilder().setActiveSessions(sessionIdMap.size());
        builder.setKeepAlive(false);
        sessionIdMap.forEach((msisdn, sessionContext) -> {
            builder.addUsers(User.newBuilder().setApn(sessionContext.getApn()).setMccMnc(sessionContext.getMccMnc()).setMsisdn(msisdn).build());
        });
        ocsgwAnalytics.sendAnalytics(builder.build());
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

        CreditControlRequestInfo creditControlRequestInfo = convertRequestToGrpc(context);
        if (creditControlRequestInfo != null) {
            ccrMap.put(context.getSessionId(), context);
            addToSessionMap(context);
            sendRequest(creditControlRequestInfo);
        } else {
            // ToDo : Send diameter failure to P-GW.
        }
    }

    private synchronized void sendRequest(CreditControlRequestInfo requestInfo) {
        if (creditControlRequest != null) {
            try {
                creditControlRequest.onNext(requestInfo);
            } catch (Exception e) {
                LOG.error("Failed to send Request", e);
            }
        } else {
            LOG.warn("[!!] creditControlRequest is null");
        }
    }

    private CreditControlAnswer createCreditControlAnswer(CreditControlAnswerInfo response) {
        if (response == null) {
            LOG.error("Empty CreditControlAnswerInfo received");
            return new CreditControlAnswer(org.ostelco.diameter.model.ResultCode.DIAMETER_UNABLE_TO_COMPLY, new ArrayList<>());
        }

        final LinkedList<MultipleServiceCreditControl> multipleServiceCreditControls = new LinkedList<>();
        for (org.ostelco.ocs.api.MultipleServiceCreditControl mscc : response.getMsccList()) {
            multipleServiceCreditControls.add(GrpcDiameterConverter.convertMSCC(mscc));
        }
        return new CreditControlAnswer(GrpcDiameterConverter.convertResultCode(response.getResultCode()), multipleServiceCreditControls);
    }


    @Override
    public boolean isBlocked(final String msisdn) {
        return blocked.contains(msisdn);
    }
}
