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

    private static final Logger logger = LoggerFactory.getLogger(GrpcDataSource.class);

    /**
     * The ip:port on which the gRPC service will be receiving incoming
     * connections.
     */
    private static final String SOCKET = "172.16.238.4:8082";  // ToDo : move to config file

    private final OcsServiceGrpc.OcsServiceStub ocsServiceStub;

    private StreamObserver<FetchDataBucketInfo> fetchDataBucketRequests;

    private StreamObserver<ReturnUnusedDataRequest> returnUnusedDataRequests;

    private static final int MAX_ENTRIES = 300;
    private final LinkedHashMap<String, CreditControlRequestContext> ccrMap = new LinkedHashMap<String, CreditControlRequestContext>(MAX_ENTRIES, .75F) {
        protected boolean removeEldestEntry(Map.Entry<String, CreditControlRequestContext> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private abstract class AbstactObserver<T> implements StreamObserver<T> {
        public final void onError(Throwable t) {
            init();
        }

        public final void onCompleted() {
        }
    }

    public GrpcDataSource() {
        // Set up a channel to be used to communicate as an OCS instance, to a
        // grpc instance.
        final ManagedChannel channel = ManagedChannelBuilder
                .forTarget(SOCKET)
                .usePlaintext(true) // disable encryption for testing
                .build();

        // Initialize the stub that will be used to actually
        // communicate from the client emulating being the OCS.
        ocsServiceStub = OcsServiceGrpc.newStub(channel);
    }

    @Override
    public void init() {

        fetchDataBucketRequests =
                ocsServiceStub.fetchDataBucket(
                        new AbstactObserver<FetchDataBucketInfo>() {
                            public void onNext(FetchDataBucketInfo response) {
                                try {
                                    logger.info("[<<] Received data bucket of " + response.getBytes() + " bytes for " + response.getMsisdn());
                                    final CreditControlRequestContext ccrContext = ccrMap.remove(response.getRequestId());
                                    if (ccrContext != null) {
                                        final ServerCCASession session = ccrContext.getSession();
                                        if (session != null) {
                                            CreditControlAnswer answer = createCreditControlAnswer(ccrContext, response);
                                            ccrContext.sendCreditControlAnswer(answer);
                                        } else {
                                            logger.warn("No stored CCR or Session for " + response.getRequestId());
                                        }
                                    } else {
                                        logger.warn("Missing CreditControlRequestContext for req id " + response.getRequestId());
                                    }
                                } catch (Exception e) {
                                    logger.error("fetchDataBucket failed ", e);
                                }
                            }
                        });

        returnUnusedDataRequests = ocsServiceStub.returnUnusedData(
                new AbstactObserver<ReturnUnusedDataResponse>() {
                    public void onNext(ReturnUnusedDataResponse response) {
                        try {
                            System.out.println("[<<] Returned data bucket for " + response.getMsisdn());
                        } catch (Exception e) {
                            logger.error("returnUnusedData failed ", e);
                        }
                    }
                });

    }

    @Override
    public void handleRequest(CreditControlRequestContext context) {

        switch (context.getOriginalCreditControlRequest().getRequestTypeAVPValue()) {

            case RequestType.INITIAL_REQUEST:
            case RequestType.UPDATE_REQUEST:
                final String requestId = UUID.randomUUID().toString();
                ccrMap.put(requestId, context);
                logger.info("[>>] Requesting bytes for " + context.getCreditControlRequest().getMsisdn());
                if (fetchDataBucketRequests != null) {
                    fetchDataBucketRequests.onNext(FetchDataBucketInfo.newBuilder()
                            .setMsisdn(context.getCreditControlRequest().getMsisdn())
                            .setBytes(context.getCreditControlRequest().getRequestedUnits()) // ToDo: this should correspond to a the correct MSCC
                            .setRequestId(requestId)
                            .build());
                } else {
                    logger.warn("[!!] fetchDataBucketRequests is null");
                }
                break;
            case RequestType.TERMINATION_REQUEST:
                // For terminate we do not need to send to remote end before we send CCA back (no reservation)
                if (returnUnusedDataRequests != null) {
                    returnUnusedDataRequests.onNext(ReturnUnusedDataRequest.newBuilder()
                            .setMsisdn(context.getCreditControlRequest().getMsisdn())
                            .setBytes(0) // ToDo : Fix proper
                            .build());
                }
                break;
            default:
                logger.info("Unhandled forward request");
        }
    }

    private CreditControlAnswer createCreditControlAnswer(CreditControlRequestContext context, FetchDataBucketInfo response) {

        // ToDo: Update with info in reply, this is just a temporary solution where we threat all
        //       mscc the same.

        CreditControlRequest request = context.getCreditControlRequest();
        CreditControlAnswer answer = new CreditControlAnswer();

        final LinkedList<MultipleServiceCreditControl> multipleServiceCreditControls = request.getMultipleServiceCreditControls();

        for (MultipleServiceCreditControl mscc : multipleServiceCreditControls) {
            mscc.setGrantedServiceUnit(response.getBytes());
        }

        return answer;
    }
}
