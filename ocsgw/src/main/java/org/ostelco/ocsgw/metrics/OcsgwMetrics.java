package org.ostelco.ocsgw.metrics;

import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.ostelco.ocsgw.utils.EventConsumer;
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReply;
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReport;
import org.ostelco.prime.metrics.api.OcsgwAnalyticsServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class OcsgwMetrics {

    private static final Logger LOG = LoggerFactory.getLogger(OcsgwMetrics.class);

    private static final int KEEP_ALIVE_TIMEOUT_IN_MINUTES = 1;

    private static final int KEEP_ALIVE_TIME_IN_SECONDS = 50;

    private OcsgwAnalyticsServiceGrpc.OcsgwAnalyticsServiceStub ocsgwAnalyticsServiceStub;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture initAnalyticsFuture = null;

    private ScheduledFuture keepAliveFuture = null;

    private ScheduledFuture autoReportAnalyticsFuture = null;

    private OcsgwAnalyticsReport lastActiveSessions = null;

    private final ConcurrentLinkedQueue<OcsgwAnalyticsReport> reportQueue = new ConcurrentLinkedQueue<>();

    public OcsgwMetrics(String metricsServerHostname, ServiceAccountJwtAccessCredentials credentials) {

        try {
            final NettyChannelBuilder nettyChannelBuilder = NettyChannelBuilder
                    .forTarget(metricsServerHostname)
                    .keepAliveWithoutCalls(true)
                    .keepAliveTimeout(KEEP_ALIVE_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES)
                    .keepAliveTime(KEEP_ALIVE_TIME_IN_SECONDS, TimeUnit.SECONDS);

            final ManagedChannelBuilder channelBuilder = Files.exists(Paths.get("/cert/metrics.crt"))
                        ? nettyChannelBuilder.sslContext(GrpcSslContexts.forClient().trustManager(new File("/cert/metrics.crt")).build())
                        : nettyChannelBuilder;

            final ManagedChannel channel = channelBuilder
                    .useTransportSecurity()
                    .build();
            if (credentials != null) {
                ocsgwAnalyticsServiceStub  = OcsgwAnalyticsServiceGrpc.newStub(channel)
                        .withCallCredentials(MoreCallCredentials.from(credentials));
            } else {
                ocsgwAnalyticsServiceStub = OcsgwAnalyticsServiceGrpc.newStub(channel);
            }
        } catch (SSLException e) {
            LOG.warn("Failed to setup OcsMetrics", e);
        }
    }

    public void initAnalyticsRequest() {
        StreamObserver<OcsgwAnalyticsReport> ocsgwAnalyticsReport = ocsgwAnalyticsServiceStub.ocsgwAnalyticsEvent(
                new StreamObserver<OcsgwAnalyticsReply>() {

                    @Override
                    public void onNext(OcsgwAnalyticsReply value) {
                        // Ignore reply from Prime
                    }

                    @Override
                    public void onError(Throwable t) {
                        LOG.error("AnalyticsRequestObserver error", t);
                        if (t instanceof StatusRuntimeException) {
                            reconnectAnalyticsReport();
                        }
                    }

                    @Override
                    public void onCompleted() {
                        // Nothing to do here
                    }
                }
        );
        initKeepAlive();
        initAutoReportAnalyticsReport();

        EventConsumer<OcsgwAnalyticsReport> analyticsReportConsumer = new EventConsumer(reportQueue, ocsgwAnalyticsReport);
        new Thread(analyticsReportConsumer).start();
    }


    public void sendAnalytics(OcsgwAnalyticsReport report) {
        if (report != null) {
            queueReport(report);
        }
    }

    private void queueReport(OcsgwAnalyticsReport report) {
        try {
            reportQueue.add(report);
            synchronized (reportQueue) {
                reportQueue.notifyAll();
            }
        } catch (NullPointerException e) {
            LOG.error("Failed to queue Report", e);
        }
    }

    private void reconnectKeepAlive() {
        LOG.debug("reconnectKeepAlive called");
        if (keepAliveFuture != null) {
            keepAliveFuture.cancel(true);
        }
    }

    private void reconnectAnalyticsReport() {
        LOG.debug("reconnectAnalyticsReport called");

        if (autoReportAnalyticsFuture != null) {
            autoReportAnalyticsFuture.cancel(true);
        }

        if (initAnalyticsFuture != null) {
            initAnalyticsFuture.cancel(true);
        }

        LOG.debug("Schedule new Callable initAnalyticsRequest");
        initAnalyticsFuture = executorService.schedule((Callable<Object>) () -> {
                    reconnectKeepAlive();
                    LOG.debug("Calling initAnalyticsRequest");
                    initAnalyticsRequest();
                    sendAnalytics(lastActiveSessions);
                    return "Called!";
                },
                5,
                TimeUnit.SECONDS);
    }

    private void initAutoReportAnalyticsReport() {
        autoReportAnalyticsFuture = executorService.scheduleAtFixedRate((Runnable) () -> {
                    sendAnalytics(lastActiveSessions);
                },
                30,
                30,
                TimeUnit.MINUTES);
    }

    private void initKeepAlive() {
        // this is used to keep connection alive
        keepAliveFuture = executorService.scheduleWithFixedDelay(() -> {
                    sendAnalytics(OcsgwAnalyticsReport.newBuilder().setKeepAlive(true).build());
                },
                15,
                50,
                TimeUnit.SECONDS);
    }
}