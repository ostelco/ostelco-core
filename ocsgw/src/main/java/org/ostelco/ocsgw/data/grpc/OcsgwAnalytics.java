package org.ostelco.ocsgw.data.grpc;

import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.stub.StreamObserver;
import org.ostelco.prime.analytics.api.OcsgwAnalyticsReply;
import org.ostelco.prime.analytics.api.OcsgwAnalyticsReport;
import org.ostelco.prime.analytics.api.OcsgwAnalyticsServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class OcsgwAnalytics {

    private static final Logger LOG = LoggerFactory.getLogger(OcsgwAnalytics.class);

    private final OcsgwAnalyticsServiceGrpc.OcsgwAnalyticsServiceStub ocsgwAnalyticsServiceStub;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private StreamObserver<OcsgwAnalyticsReport> ocsgwAnalyticsReport;

    private ScheduledFuture initAnalyticsFuture = null;


    // FixMe Martin : Can not reuse channel
    public OcsgwAnalytics(ManagedChannel channel, ServiceAccountJwtAccessCredentials credentials) {
        if (credentials != null) {
            ocsgwAnalyticsServiceStub  = OcsgwAnalyticsServiceGrpc.newStub(channel)
                    .withCallCredentials(MoreCallCredentials.from(credentials));
        } else {
            ocsgwAnalyticsServiceStub = OcsgwAnalyticsServiceGrpc.newStub(channel);
        }
    }

    private abstract class AnalyticsRequestObserver<T> implements StreamObserver<T> {
        public final void onError(Throwable t) {
            LOG.error("AnalyticsRequestObserver error", t);
            if (t instanceof StatusRuntimeException) {
                reconnectAnalyticsReport();
            }
        }

        public final void onCompleted() {
            // Nothing to do here
        }
    }

    private void reconnectAnalyticsReport() {
        LOG.info("reconnectAnalyticsReport called");

        if (initAnalyticsFuture != null) {
            initAnalyticsFuture.cancel(true);
        }

        LOG.info("Schedule new Callable initAnalyticsRequest");
        initAnalyticsFuture = executorService.schedule((Callable<Object>) () -> {
                    LOG.info("Calling initAnalyticsRequest");
                    initAnalyticsRequest();
                    return "Called!";
                },
                5,
                TimeUnit.SECONDS);
    }

    public void initAnalyticsRequest() {
        ocsgwAnalyticsReport = ocsgwAnalyticsServiceStub.ocsgwAnalyticsEvent(
                new AnalyticsRequestObserver<OcsgwAnalyticsReply>() {

                    @Override
                    public void onNext(OcsgwAnalyticsReply value) {
                        // Ignore reply from Prime
                    }
                }
        );
    }

    public void sendAnalytics(int size) {
        ocsgwAnalyticsReport.onNext(OcsgwAnalyticsReport.newBuilder().setActiveSessions(size).build());
    }

}