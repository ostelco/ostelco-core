package com.telenordigital.ocsgw.data.grpc;

import com.telenordigital.ocsgw.data.DataSource;
import com.telenordigital.ostelco.diameter.CreditControlContext;
import com.telenordigital.ostelco.diameter.model.CreditControlAnswer;
import com.telenordigital.ostelco.diameter.model.FinalUnitAction;
import com.telenordigital.ostelco.diameter.model.FinalUnitIndication;
import com.telenordigital.ostelco.diameter.model.MultipleServiceCreditControl;
import com.telenordigital.ostelco.diameter.model.RedirectAddressType;
import com.telenordigital.ostelco.diameter.model.RedirectServer;
import com.telenordigital.prime.ocs.CreditControlAnswerInfo;
import com.telenordigital.prime.ocs.CreditControlRequestInfo;
import com.telenordigital.prime.ocs.CreditControlRequestType;
import com.telenordigital.prime.ocs.OcsServiceGrpc;
import com.telenordigital.prime.ocs.PsInformation;
import com.telenordigital.prime.ocs.ServiceUnit;
import com.telenordigital.prime.ocs.ServiceInfo;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.jdiameter.api.cca.ServerCCASession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.telenordigital.ostelco.diameter.model.RequestType.EVENT_REQUEST;
import static com.telenordigital.ostelco.diameter.model.RequestType.INITIAL_REQUEST;
import static com.telenordigital.ostelco.diameter.model.RequestType.TERMINATION_REQUEST;
import static com.telenordigital.ostelco.diameter.model.RequestType.UPDATE_REQUEST;

/**
 * Uses gRPC to fetch data remotely
 *
 */
public class GrpcDataSource implements DataSource {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcDataSource.class);

    private final OcsServiceGrpc.OcsServiceStub ocsServiceStub;

    private final Set<String> blocked = new HashSet<>();

    private StreamObserver<CreditControlRequestInfo> creditControlRequest;

    private static final int MAX_ENTRIES = 300;
    private final LinkedHashMap<String, CreditControlContext> ccrMap = new LinkedHashMap<String, CreditControlContext>(MAX_ENTRIES, .75F) {
        protected boolean removeEldestEntry(Map.Entry<String, CreditControlContext> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private abstract class AbstactObserver<T> implements StreamObserver<T> {
        public final void onError(Throwable t) {
            LOG.error("We got an error", t);
        }

        public final void onCompleted() {
            // Nothing to do here
            LOG.info("It seems to be completed") ;
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
                                final ServerCCASession session = ccrContext.getSession();
                                if (session != null) {
                                    CreditControlAnswer cca = createCreditControlAnswer(answer);
                                    ccrContext.sendCreditControlAnswer(cca);
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
    }

    @Override
    public void handleRequest(final CreditControlContext context) {
        final String requestId = UUID.randomUUID().toString();
        ccrMap.put(requestId, context);
        LOG.info("[>>] Requesting bytes for {}", context.getCreditControlRequest().getMsisdn());

        if (creditControlRequest != null) {
            try {
                CreditControlRequestInfo.Builder builder = CreditControlRequestInfo.newBuilder();
                builder.setType(getRequestType(context));
                for (MultipleServiceCreditControl mscc : context.getCreditControlRequest().getMultipleServiceCreditControls()) {
                    builder.addMscc(com.telenordigital.prime.ocs.MultipleServiceCreditControl.newBuilder()
                            .setRequested(ServiceUnit.newBuilder()
                                    .setInputOctets(0L)
                                    .setOutputOctetes(0L)
                                    .setTotalOctets(mscc.getRequestedUnits())
                                    .build())
                            .setUsed(ServiceUnit.newBuilder()
                                    .setInputOctets(mscc.getUsedUnitsInput())
                                    .setOutputOctetes(mscc.getUsedUnitsOutput())
                                    .setTotalOctets(mscc.getUsedUnitsTotal())
                                    .build())
                            .setRatingGroup(mscc.getRatingGroup())
                            .setServiceIdentifier(mscc.getServiceIdentifier())
                    );
                }
                creditControlRequest.onNext(builder
                        .setRequestId(requestId)
                        .setMsisdn(context.getCreditControlRequest().getMsisdn())
                        .setImsi(context.getCreditControlRequest().getImsi())
                        .setServiceInformation(
                                ServiceInfo.newBuilder().setPsInformation(
                                        PsInformation.newBuilder()
                                                .setCalledStationId(context.getCreditControlRequest().getServiceInformation().getPsInformation().getCalledStationId())
                                                .setSgsnMccMnc(context.getCreditControlRequest().getServiceInformation().getPsInformation().getSgsnMncMcc())
                                ).build())
                        .build());
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
        for (com.telenordigital.prime.ocs.MultipleServiceCreditControl mscc : response.getMsccList() ) {
            multipleServiceCreditControls.add(convertMSCC(mscc));
            updateBlockedList(mscc, response.getMsisdn());
        }
        return new CreditControlAnswer(multipleServiceCreditControls);
    }

    private void updateBlockedList(com.telenordigital.prime.ocs.MultipleServiceCreditControl msccGRPC, String msisdn) {
        // This suffers from the fact that one Credit-Control-Request can have multiple MSCC
        if (msccGRPC != null && msisdn != null) {
            if (msccGRPC.getGranted().getTotalOctets() == 0) {
                blocked.add(msisdn);
            } else {
                blocked.remove(msisdn);
            }
        }
    }

    private MultipleServiceCreditControl convertMSCC(com.telenordigital.prime.ocs.MultipleServiceCreditControl msccGRPC) {
        return new MultipleServiceCreditControl(
                msccGRPC.getRatingGroup(),
                msccGRPC.getServiceIdentifier(),
                0,
                0,
                0,
                0,
                msccGRPC.getGranted().getTotalOctets(),
                msccGRPC.getValidityTime(),
                convertFinalUnitIndication(msccGRPC.getFinalUnitIndication()));
    }

    private FinalUnitIndication convertFinalUnitIndication(com.telenordigital.prime.ocs.FinalUnitIndication fuiGrpc) {
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
