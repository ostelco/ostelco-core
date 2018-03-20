package org.ostelco.ocsgw.data.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.cca.ServerCCASession;
import org.ostelco.diameter.CreditControlContext;
import org.ostelco.diameter.SessionContext;
import org.ostelco.diameter.model.CreditControlAnswer;
import org.ostelco.diameter.model.FinalUnitAction;
import org.ostelco.diameter.model.FinalUnitIndication;
import org.ostelco.diameter.model.MultipleServiceCreditControl;
import org.ostelco.diameter.model.RedirectAddressType;
import org.ostelco.diameter.model.RedirectServer;
import org.ostelco.ocs.api.ActivateRequest;
import org.ostelco.ocs.api.ActivateResponse;
import org.ostelco.ocs.api.CreditControlAnswerInfo;
import org.ostelco.ocs.api.CreditControlRequestInfo;
import org.ostelco.ocs.api.CreditControlRequestType;
import org.ostelco.ocs.api.OcsServiceGrpc;
import org.ostelco.ocs.api.PsInformation;
import org.ostelco.ocs.api.ServiceInfo;
import org.ostelco.ocs.api.ServiceUnit;
import org.ostelco.ocsgw.OcsServer;
import org.ostelco.ocsgw.data.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import static org.ostelco.diameter.model.RequestType.EVENT_REQUEST;
import static org.ostelco.diameter.model.RequestType.INITIAL_REQUEST;
import static org.ostelco.diameter.model.RequestType.TERMINATION_REQUEST;
import static org.ostelco.diameter.model.RequestType.UPDATE_REQUEST;

/**
 * Uses gRPC to fetch data remotely
 *
 */
public class GrpcDataSource implements DataSource {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcDataSource.class);

    private final OcsServiceGrpc.OcsServiceStub ocsServiceStub;

    private final Set<String> blocked = new HashSet<>();

    private StreamObserver<CreditControlRequestInfo> creditControlRequest;

    private static final int MAX_ENTRIES = 3000;
    private final LinkedHashMap<String, CreditControlContext> ccrMap = new LinkedHashMap<String, CreditControlContext>(MAX_ENTRIES, .75F) {
        protected boolean removeEldestEntry(Map.Entry<String, CreditControlContext> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private final LinkedHashMap<String, SessionContext> sessionIdMap = new LinkedHashMap<String, SessionContext>(MAX_ENTRIES, .75F) {
        protected boolean removeEldestEntry(Map.Entry<String, SessionContext> eldest) {
            return size() > MAX_ENTRIES;
        }
    };


    private abstract class AbstactObserver<T> implements StreamObserver<T> {
        public final void onError(Throwable t) {
            LOG.error("We got an error", t);
        }

        public final void onCompleted() {
            // Nothing to do here
            LOG.info("It seems to be completed");
        }
    }

    public GrpcDataSource(String target, boolean encrypted) {

        LOG.info("Created GrpcDataSource");
        LOG.info("target : {}", target);
        LOG.info("encrypted : {}", encrypted);
        // Set up a channel to be used to communicate as an OCS instance,
        // to a gRPC instance.
        final ManagedChannel channel = ManagedChannelBuilder
                .forTarget(target)
                .usePlaintext(!encrypted)
                .build();

        // Initialize the stub that will be used to actually
        // communicate from the client emulating being the OCS.
        ocsServiceStub = OcsServiceGrpc.newStub(channel);
    }

    @Override
    public void init() {

        creditControlRequest = ocsServiceStub.creditControlRequest(
                new AbstactObserver<CreditControlAnswerInfo>() {
                    public void onNext(CreditControlAnswerInfo answer) {
                        try {
                            LOG.info("[<<] Received data bucket for " + answer.getMsisdn());
                            final CreditControlContext ccrContext = ccrMap.remove(answer.getRequestId());
                            if (ccrContext != null) {
                                final ServerCCASession session = OcsServer.getInstance().getStack().getSession(ccrContext.getSessionId(), ServerCCASession.class);
                                if (session != null && session.isValid()) {
                                    CreditControlAnswer cca = createCreditControlAnswer(answer);
                                    try {
                                        session.sendCreditControlAnswer(ccrContext.createCCA(cca));
                                    } catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException e) {
                                        LOG.error("Failed to send Credit-Control-Answer", e);
                                    }
                                } else {
                                    LOG.warn("No stored CCR or Session for " + answer.getRequestId());
                                }
                            } else {
                                LOG.warn("Missing CreditControlContext for req id " + answer.getRequestId());
                            }
                        } catch (Exception e) {
                            LOG.error("Credit-Control-Request failed ", e);
                        }
                    }
                });

        ActivateRequest dummyActivate = ActivateRequest.newBuilder().build();
        ocsServiceStub.activate(dummyActivate, new AbstactObserver<ActivateResponse>() {
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

    @Override
    public void handleRequest(final CreditControlContext context) {
        ccrMap.put(context.getSessionId(), context);
        sessionIdMap.put(context.getCreditControlRequest().getMsisdn(), new SessionContext(context.getSessionId(), context.getOriginHost(), context.getOriginRealm()));
        LOG.info("[>>] Requesting bytes for {}", context.getCreditControlRequest().getMsisdn());

        if (creditControlRequest != null) {
            try {
                CreditControlRequestInfo.Builder builder = CreditControlRequestInfo
                        .newBuilder()
                        .setType(getRequestType(context));
                for (MultipleServiceCreditControl mscc : context.getCreditControlRequest().getMultipleServiceCreditControls()) {
                    builder.addMscc(org.ostelco.ocs.api.MultipleServiceCreditControl.newBuilder()
                            .setRequested(ServiceUnit.newBuilder()
                                    .setInputOctets(0L)
                                    .setOutputOctetes(0L)
                                    .setTotalOctets(mscc.getRequested().getTotal())
                                    .build())
                            .setUsed(ServiceUnit.newBuilder()
                                    .setInputOctets(mscc.getRequested().getInput())
                                    .setOutputOctetes(mscc.getRequested().getOutput())
                                    .setTotalOctets(mscc.getRequested().getTotal())
                                    .build())
                            .setRatingGroup(mscc.getRatingGroup())
                            .setServiceIdentifier(mscc.getServiceIdentifier())
                    );
                }

                builder.setRequestId(context.getSessionId())
                        .setMsisdn(context.getCreditControlRequest().getMsisdn())
                        .setImsi(context.getCreditControlRequest().getImsi());

                if (context.getCreditControlRequest().getServiceInformation() != null) {
                    final org.ostelco.diameter.model.PsInformation psInformation
                            = context.getCreditControlRequest().getServiceInformation().getPsInformation();

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
            updateBlockedList(mscc, response.getMsisdn());
        }
        return new CreditControlAnswer(multipleServiceCreditControls);
    }

    private void updateBlockedList(org.ostelco.ocs.api.MultipleServiceCreditControl msccGRPC, String msisdn) {
        // This suffers from the fact that one Credit-Control-Request can have multiple MSCC
        if (msccGRPC != null && msisdn != null) {
            if (msccGRPC.getGranted().getTotalOctets() < msccGRPC.getRequested().getTotalOctets()) {
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
                new org.ostelco.diameter.model.ServiceUnit(),
                new org.ostelco.diameter.model.ServiceUnit(),
                new org.ostelco.diameter.model.ServiceUnit(msccGRPC.getGranted().getTotalOctets(), 0, 0),
                msccGRPC.getValidityTime(),
                convertFinalUnitIndication(msccGRPC.getFinalUnitIndication()));
    }

    private FinalUnitIndication convertFinalUnitIndication(org.ostelco.ocs.api.FinalUnitIndication fuiGrpc) {
        return new FinalUnitIndication(
                FinalUnitAction.values()[fuiGrpc.getFinalUnitAction().getNumber()],
                new LinkedList<>(),
                fuiGrpc.getFilterIdList(),
                new RedirectServer(RedirectAddressType.IPV4_ADDRESS));
    }

    @Override
    public boolean isBlocked(final String msisdn) {
        return blocked.contains(msisdn);
    }
}
