package org.ostelco.ocsgw.data.grpc;

import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.stub.StreamObserver;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.ostelco.diameter.CreditControlContext;
import org.ostelco.diameter.model.*;
import org.ostelco.ocs.api.ActivateRequest;
import org.ostelco.ocs.api.ActivateResponse;
import org.ostelco.ocs.api.CreditControlAnswerInfo;
import org.ostelco.ocs.api.CreditControlRequestInfo;
import org.ostelco.ocs.api.CreditControlRequestType;
import org.ostelco.ocs.api.OcsServiceGrpc;
import org.ostelco.ocs.api.PsInformation;
import org.ostelco.ocs.api.ReportingReason;
import org.ostelco.ocs.api.ServiceInfo;
import org.ostelco.ocs.api.ServiceUnit;
import org.ostelco.ocsgw.OcsServer;
import org.ostelco.ocsgw.data.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static org.ostelco.diameter.model.RequestType.EVENT_REQUEST;
import static org.ostelco.diameter.model.RequestType.INITIAL_REQUEST;
import static org.ostelco.diameter.model.RequestType.TERMINATION_REQUEST;
import static org.ostelco.diameter.model.RequestType.UPDATE_REQUEST;

/**
 * Uses gRPC to fetch data remotely
 */
public class GrpcDataSource implements DataSource {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcDataSource.class);

    private final OcsServiceGrpc.OcsServiceStub ocsServiceStub;

    private final Set<String> blocked = new HashSet<>();

    private StreamObserver<CreditControlRequestInfo> creditControlRequest;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture keepAliveFuture = null;

    private ScheduledFuture initActivateFuture = null;

    private ScheduledFuture initCCRFuture = null;

    private static final int MAX_ENTRIES = 3000;
    private final LinkedHashMap<String, CreditControlContext> ccrMap = new LinkedHashMap<String, CreditControlContext>(MAX_ENTRIES, .75F) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CreditControlContext> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private final LinkedHashMap<String, SessionContext> sessionIdMap = new LinkedHashMap<String, SessionContext>(MAX_ENTRIES, .75F) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SessionContext> eldest) {
            return size() > MAX_ENTRIES;
        }
    };


    private abstract class CreditControlRequestObserver<T> implements StreamObserver<T> {
        public final void onError(Throwable t) {
            LOG.error("CreditControlRequestObserver error", t);
            if (t instanceof StatusRuntimeException) {
                reconnectCreditControlRequest();
            }
        }

        public final void onCompleted() {
            // Nothing to do here
        }
    }

    private abstract class ActivateObserver<T> implements StreamObserver<T> {
        public final void onError(Throwable t) {
            LOG.error("ActivateObserver error", t);
            if (t instanceof StatusRuntimeException) {
                reconnectActivate();
            }
        }

        public final void onCompleted() {
            // Nothing to do here
        }
    }

    private void reconnectActivate() {
        LOG.info("reconnectActivate called");

        if (initActivateFuture == null || initActivateFuture.isDone()) {
            LOG.info("Schedule new Callable initActivate");
            initActivateFuture = executorService.schedule((Callable<Object>) () -> {
                        LOG.info("Calling initActivate");
                        initActivate();
                        return "Called!";
                    },
                    5,
                    TimeUnit.SECONDS);
        }
    }

    private void reconnectCreditControlRequest() {
        LOG.info("reconnectCreditControlRequest called");
        if (keepAliveFuture != null) {
            keepAliveFuture.cancel(true);
        }

        if (initCCRFuture == null || initCCRFuture.isDone()) {
            LOG.info("Schedule new Callable initCreditControlRequest");
            initCCRFuture = executorService.schedule((Callable<Object>) () -> {
                        LOG.info("Calling initCreditControlRequest");
                        initCreditControlRequest();
                        return "Called!";
                    },
                    5,
                    TimeUnit.SECONDS);
        }
    }

    public GrpcDataSource(final String target, final boolean encrypted) throws IOException {

        LOG.info("Created GrpcDataSource");
        LOG.info("target : {}", target);
        LOG.info("encrypted : {}", encrypted);
        // Set up a channel to be used to communicate as an OCS instance,
        // to a gRPC instance.
        final ManagedChannelBuilder channelBuilder = ManagedChannelBuilder
                .forTarget(target)
                .keepAliveWithoutCalls(true)
                .keepAliveTimeout(1, TimeUnit.MINUTES)
                .keepAliveTime(50, TimeUnit.SECONDS);

        // Initialize the stub that will be used to actually
        // communicate from the client emulating being the OCS.
        if (encrypted) {
            final String serviceAccountFile = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            final ServiceAccountJwtAccessCredentials credentials =
                    ServiceAccountJwtAccessCredentials.fromStream(new FileInputStream(serviceAccountFile));
            final ManagedChannel channel = channelBuilder
                    .usePlaintext(true) // FIXME enable TLS and then remove this
                    .build();
            ocsServiceStub = OcsServiceGrpc.newStub(channel)
                    .withCallCredentials(MoreCallCredentials.from(credentials));
        } else {
            final ManagedChannel channel = channelBuilder
                    .usePlaintext(true)
                    .build();
            ocsServiceStub = OcsServiceGrpc.newStub(channel);
        }
    }

    @Override
    public void init() {

        initCreditControlRequest();

        initActivate();

        initKeepAlive();
    }

    private void initCreditControlRequest() {
        creditControlRequest = ocsServiceStub.creditControlRequest(
                new CreditControlRequestObserver<CreditControlAnswerInfo>() {
                    public void onNext(CreditControlAnswerInfo answer) {
                        try {
                            LOG.info("[<<] Received data bucket for {}", answer.getMsisdn());
                            final CreditControlContext ccrContext = ccrMap.remove(answer.getRequestId());
                            if (ccrContext != null) {
                                final ServerCCASession session = OcsServer.getInstance().getStack().getSession(ccrContext.getSessionId(), ServerCCASession.class);
                                if (session != null && session.isValid()) {
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
                });
    }

    private void initActivate() {
        ActivateRequest dummyActivate = ActivateRequest.newBuilder().build();
        ocsServiceStub.activate(dummyActivate, new ActivateObserver<ActivateResponse>() {
            @Override
            public void onNext(ActivateResponse activateResponse) {
                LOG.info("Active user {}", activateResponse.getMsisdn());
                if (sessionIdMap.containsKey(activateResponse.getMsisdn())) {
                    final SessionContext sessionContext = sessionIdMap.get(activateResponse.getMsisdn());
                    OcsServer.getInstance().sendReAuthRequest(sessionContext);
                } else {
                    LOG.info("No session context stored for msisdn : {}", activateResponse.getMsisdn());
                }
            }
        });
    }

    private void initKeepAlive() {
        // this is just to keep connection alive
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

    private void updateBlockedList(CreditControlAnswerInfo answer, CreditControlRequest request) {
        // This suffers from the fact that one Credit-Control-Request can have multiple MSCC
        for (org.ostelco.ocs.api.MultipleServiceCreditControl msccAnswer : answer.getMsccList()) {
            for (org.ostelco.diameter.model.MultipleServiceCreditControl msccRequest: request.getMultipleServiceCreditControls()) {
                if ((msccAnswer.getServiceIdentifier() == msccRequest.getServiceIdentifier()) && (msccAnswer.getRatingGroup() == msccRequest.getRatingGroup())) {
                    updateBlockedList(msccAnswer, msccRequest, answer.getMsisdn());
                    return;
                }
            }
        }
    }

    @Override
    public void handleRequest(final CreditControlContext context) {
        ccrMap.put(context.getSessionId(), context);
        sessionIdMap.put(context.getCreditControlRequest().getMsisdn(), new SessionContext(context.getSessionId(), context.getCreditControlRequest().getOriginHost(), context.getCreditControlRequest().getOriginRealm()));
        LOG.info("[>>] Requesting bytes for {}", context.getCreditControlRequest().getMsisdn());

        if (creditControlRequest != null) {
            try {
                CreditControlRequestInfo.Builder builder = CreditControlRequestInfo
                        .newBuilder()
                        .setType(getRequestType(context));

                for (MultipleServiceCreditControl mscc : context.getCreditControlRequest().getMultipleServiceCreditControls()) {

                    org.ostelco.ocs.api.MultipleServiceCreditControl.Builder protoMscc = org.ostelco.ocs.api.MultipleServiceCreditControl.newBuilder();

                    if (!mscc.getRequested().isEmpty()) {

                        org.ostelco.diameter.model.ServiceUnit requested = mscc.getRequested().get(0);

                        protoMscc.setRequested(ServiceUnit.newBuilder()
                                .setInputOctets(0L)
                                .setOutputOctetes(0L)
                                .setTotalOctets(requested.getTotal())
                                .build());
                    }


                    org.ostelco.diameter.model.ServiceUnit used = mscc.getUsed();

                    protoMscc.setUsed(ServiceUnit.newBuilder()
                            .setInputOctets(used.getInput())
                            .setOutputOctetes(used.getOutput())
                            .setTotalOctets(used.getTotal())
                            .build());

                    protoMscc.setRatingGroup(mscc.getRatingGroup());
                    protoMscc.setServiceIdentifier(mscc.getServiceIdentifier());

                    if (mscc.getReportingReason() != null) {
                        protoMscc.setReportingReasonValue(mscc.getReportingReason().ordinal());
                    } else {
                        protoMscc.setReportingReasonValue(ReportingReason.UNRECOGNIZED.ordinal());
                    }
                    builder.addMscc(protoMscc.build());
                }

                builder.setRequestId(context.getSessionId())
                        .setMsisdn(context.getCreditControlRequest().getMsisdn())
                        .setImsi(context.getCreditControlRequest().getImsi());

                if (!context.getCreditControlRequest().getServiceInformation().isEmpty()) {
                    final org.ostelco.diameter.model.PsInformation psInformation
                            = context.getCreditControlRequest().getServiceInformation().get(0).getPsInformation().get(0);

                    if (psInformation != null
                            && psInformation.getCalledStationId() != null
                            && psInformation.getSgsnMncMcc() != null) {

                        builder.setServiceInformation(
                                ServiceInfo.newBuilder()
                                        .setPsInformation(PsInformation.newBuilder()
                                                .setCalledStationId(psInformation.getCalledStationId())
                                                .setSgsnMccMnc(psInformation.getSgsnMncMcc())
                                                .build()).build());
                    }
                }
                creditControlRequest.onNext(builder.build());

            } catch (Exception e) {
                LOG.error("What just happened", e);
            }
        } else {
            LOG.warn("[!!] creditControlRequest is null");
        }
    }

    private CreditControlRequestType getRequestType(CreditControlContext context) {
        CreditControlRequestType type = CreditControlRequestType.NONE;
        switch (context.getOriginalCreditControlRequest().getRequestTypeAVPValue()) {
            case INITIAL_REQUEST:
                type = CreditControlRequestType.INITIAL_REQUEST;
                break;
            case UPDATE_REQUEST:
                type = CreditControlRequestType.UPDATE_REQUEST;
                break;
            case TERMINATION_REQUEST:
                type = CreditControlRequestType.TERMINATION_REQUEST;
                break;
            case EVENT_REQUEST:
                type = CreditControlRequestType.EVENT_REQUEST;
                break;
            default:
                LOG.warn("Unknown request type");
                break;
        }
        return type;
    }

    private CreditControlAnswer createCreditControlAnswer(CreditControlAnswerInfo response) {
        if (response == null) {
            LOG.error("Empty CreditControlAnswerInfo received");
            return new CreditControlAnswer(new ArrayList<>());
        }

        final LinkedList<MultipleServiceCreditControl> multipleServiceCreditControls = new LinkedList<>();
        for (org.ostelco.ocs.api.MultipleServiceCreditControl mscc : response.getMsccList()) {
            multipleServiceCreditControls.add(convertMSCC(mscc));
        }
        return new CreditControlAnswer(multipleServiceCreditControls);
    }

    private void updateBlockedList(org.ostelco.ocs.api.MultipleServiceCreditControl msccAnswer, org.ostelco.diameter.model.MultipleServiceCreditControl msccRequest, String msisdn) {
        if (!msccRequest.getRequested().isEmpty()) {
            if (msccAnswer.getGranted().getTotalOctets() < msccRequest.getRequested().get(0).getTotal()) {
                blocked.add(msisdn);
            } else {
                blocked.remove(msisdn);
            }
        }
    }

    private MultipleServiceCreditControl convertMSCC(org.ostelco.ocs.api.MultipleServiceCreditControl msccGRPC) {
        return new MultipleServiceCreditControl(
                msccGRPC.getRatingGroup(),
                (int) msccGRPC.getServiceIdentifier(),
                Collections.singletonList(new org.ostelco.diameter.model.ServiceUnit()),
                new org.ostelco.diameter.model.ServiceUnit(),
                new org.ostelco.diameter.model.ServiceUnit(msccGRPC.getGranted().getTotalOctets(), 0, 0),
                msccGRPC.getValidityTime(),
                convertFinalUnitIndication(msccGRPC.getFinalUnitIndication()));
    }

    private FinalUnitIndication convertFinalUnitIndication(org.ostelco.ocs.api.FinalUnitIndication fuiGrpc) {
        if (!fuiGrpc.getIsSet()) {
            return null;
        }
        return new FinalUnitIndication(
                FinalUnitAction.values()[fuiGrpc.getFinalUnitAction().getNumber()],
                fuiGrpc.getRestrictionFilterRuleList(),
                fuiGrpc.getFilterIdList(),
                new RedirectServer(
                        RedirectAddressType.values()[fuiGrpc.getRedirectServer().getRedirectAddressType().getNumber()],
                        fuiGrpc.getRedirectServer().getRedirectServerAddress()
                )
        );
    }

    @Override
    public boolean isBlocked(final String msisdn) {
        return blocked.contains(msisdn);
    }
}
