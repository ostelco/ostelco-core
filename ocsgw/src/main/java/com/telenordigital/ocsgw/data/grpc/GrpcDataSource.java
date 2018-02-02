package com.telenordigital.ocsgw.data.grpc;

import com.telenordigital.ocsgw.diameter.*;
import com.telenordigital.ocsgw.data.DataSource;
import com.telenordigital.prime.ocs.FetchDataBucketInfo;
import com.telenordigital.prime.ocs.OcsServiceGrpc;
import com.telenordigital.prime.ocs.ReturnUnusedDataRequest;
import com.telenordigital.prime.ocs.ReturnUnusedDataResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.jdiameter.api.cca.ServerCCASession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

/**
 * Uses Grpc to fetch data remotely
 *
 */
public class GrpcDataSource implements DataSource {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcDataSource.class);

    private final OcsServiceGrpc.OcsServiceStub ocsServiceStub;

    private StreamObserver<FetchDataBucketInfo> fetchDataBucketRequests;

    private StreamObserver<ReturnUnusedDataRequest> returnUnusedDataRequests;

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

        LOG.info("Init was called");

        fetchDataBucketRequests =
                ocsServiceStub.fetchDataBucket(
                        new AbstactObserver<FetchDataBucketInfo>() {
                            public void onNext(FetchDataBucketInfo response) {
                                try {
                                    LOG.info("[<<] Received data bucket of " + response.getBytes() + " bytes for " + response.getMsisdn());
                                    final CreditControlContext ccrContext = ccrMap.remove(response.getRequestId());
                                    if (ccrContext != null) {
                                        final ServerCCASession session = ccrContext.getSession();
                                        if (session != null) {
                                            CreditControlAnswer answer = createCreditControlAnswer(ccrContext, response);
                                            ccrContext.sendCreditControlAnswer(answer);
                                        } else {
                                            LOG.warn("No stored CCR or Session for " + response.getRequestId());
                                        }
                                    } else {
                                        LOG.warn("Missing CreditControlContext for req id " + response.getRequestId());
                                    }
                                } catch (Exception e) {
                                    LOG.error("fetchDataBucket failed ", e);
                                }
                            }
                        });

        returnUnusedDataRequests = ocsServiceStub.returnUnusedData(
                new AbstactObserver<ReturnUnusedDataResponse>() {
                    public void onNext(ReturnUnusedDataResponse response) {
                        try {
                            System.out.println("[<<] Returned data bucket for " + response.getMsisdn());
                        } catch (Exception e) {
                            LOG.error("returnUnusedData failed ", e);
                        }
                    }
                });

    }

    @Override
    public void handleRequest(CreditControlContext context) {

        switch (context.getOriginalCreditControlRequest().getRequestTypeAVPValue()) {

            case RequestType.INITIAL_REQUEST:
                handleInitialRequest(context);
                break;
            case RequestType.UPDATE_REQUEST:
                handleUpdateRequest(context);
                break;
            case RequestType.TERMINATION_REQUEST:
                handleTerminationRequest(context);
                break;
            default:
                LOG.info("Unhandled forward request");
                break;
        }
    }

    private void handleInitialRequest(final CreditControlContext context) {
        // CCR-Init is handled in same way as Update
        handleUpdateRequest(context);
    }

    private void handleUpdateRequest(final CreditControlContext context) {
        final String requestId = UUID.randomUUID().toString();
        ccrMap.put(requestId, context);
        LOG.info("[>>] Requesting bytes for {}", context.getCreditControlRequest().getMsisdn());
        if (fetchDataBucketRequests != null) {
            try {
                fetchDataBucketRequests.onNext(FetchDataBucketInfo.newBuilder()
                        .setMsisdn(context.getCreditControlRequest().getMsisdn())
                        .setBytes(context.getCreditControlRequest().getRequestedUnits()) // ToDo: this should correspond to a the correct MSCC
                        .setRequestId(requestId)
                        .build());
            } catch (Exception e) {
                LOG.error("What just happened", e);
            }
        } else {
            LOG.warn("[!!] fetchDataBucketRequests is null");
        }
    }

    private void handleTerminationRequest(final CreditControlContext context) {
        // For terminate we do not need to wait for remote end before we send CCA back (no reservation)
        if (returnUnusedDataRequests != null) {
            returnUnusedDataRequests.onNext(ReturnUnusedDataRequest.newBuilder()
                    .setMsisdn(context.getCreditControlRequest().getMsisdn())
                    .setBytes(1L) // ToDo : Fix proper
                    .build());
        } else {
            LOG.warn("[!!] fetchDataBucketRequests is null");
        }

        context.sendCreditControlAnswer(createCreditControlAnswer(context, null));
    }

    private CreditControlAnswer createCreditControlAnswer(CreditControlContext context, FetchDataBucketInfo response) {

        // ToDo: Update with info in reply, this is just a temporary solution where we threat all mscc the same.

        CreditControlRequest request = context.getCreditControlRequest();
        CreditControlAnswer answer = new CreditControlAnswer();

        final LinkedList<MultipleServiceCreditControl> multipleServiceCreditControls = request.getMultipleServiceCreditControls();

        for (MultipleServiceCreditControl mscc : multipleServiceCreditControls) {
            if (response != null) {
                mscc.setGrantedServiceUnit(response.getBytes());
            } else {
                mscc.setGrantedServiceUnit(0L);
            }
        }

        answer.setMultipleServiceCreditControls(multipleServiceCreditControls);

        return answer;
    }
}
