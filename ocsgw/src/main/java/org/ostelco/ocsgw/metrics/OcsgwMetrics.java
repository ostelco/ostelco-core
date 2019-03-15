package org.ostelco.ocsgw.metrics;

import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.ostelco.ocsgw.datasource.protobuf.ProtobufDataSource;
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReply;
import org.ostelco.prime.metrics.api.OcsgwAnalyticsReport;
import org.ostelco.prime.metrics.api.OcsgwAnalyticsServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OcsgwMetrics {

    private static final Logger LOG = LoggerFactory.getLogger(OcsgwMetrics.class);

    private static final int KEEP_ALIVE_TIMEOUT_IN_MINUTES = 1;

    private static final int KEEP_ALIVE_TIME_IN_MINUTES = 5;

    private OcsgwAnalyticsServiceGrpc.OcsgwAnalyticsServiceStub ocsgwAnalyticsServiceStub;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture initAnalyticsFuture = null;

    private ScheduledFuture sendAnalyticsFuture = null;

    private StreamObserver<OcsgwAnalyticsReport> ocsgwAnalyticsReportStream;

    private ManagedChannel grpcChannel;

    private ServiceAccountJwtAccessCredentials credentials;

    private String metricsServerHostname;

    private ProtobufDataSource protobufDataSource;

    public OcsgwMetrics(
            String metricsServerHostname,
            ServiceAccountJwtAccessCredentials serviceAccountJwtAccessCredentials,
            ProtobufDataSource protobufDataSource) {

        this.protobufDataSource = protobufDataSource;
        credentials = serviceAccountJwtAccessCredentials;
        this.metricsServerHostname = metricsServerHostname;
    }

    private void setupChannel() {

        ManagedChannelBuilder channelBuilder;

        final boolean disableTls = Boolean.valueOf(System.getenv("DISABLE_TLS"));

        try {
            if (disableTls) {
                channelBuilder = ManagedChannelBuilder
                        .forTarget(metricsServerHostname)
                        .usePlaintext();
            } else {
                final NettyChannelBuilder nettyChannelBuilder = NettyChannelBuilder
                        .forTarget(metricsServerHostname);


                    channelBuilder = Files.exists(Paths.get("/cert/metrics.crt"))
                            ? nettyChannelBuilder.sslContext(
                            GrpcSslContexts.forClient().trustManager(new File("/cert/metrics.crt")).build())
                            .useTransportSecurity()
                            :nettyChannelBuilder.useTransportSecurity();

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
                    .keepAliveTimeout(KEEP_ALIVE_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES)
                    .keepAliveTime(KEEP_ALIVE_TIME_IN_MINUTES, TimeUnit.MINUTES)
                    .build();

            ocsgwAnalyticsServiceStub = OcsgwAnalyticsServiceGrpc.newStub(grpcChannel)
                    .withCallCredentials(MoreCallCredentials.from(credentials));;

        } catch (SSLException e) {
            LOG.warn("Failed to setup gRPC channel", e);
        }
    }

    public void initAnalyticsRequestStream() {

        setupChannel();

        ocsgwAnalyticsReportStream = ocsgwAnalyticsServiceStub.ocsgwAnalyticsEvent(
                new StreamObserver<OcsgwAnalyticsReply>() {

                    @Override
                    public void onNext(OcsgwAnalyticsReply value) {
                        // Ignore reply from Prime
                    }

                    @Override
                    public void onError(Throwable t) {
                        LOG.error("AnalyticsRequestObserver error", t);
                        if (t instanceof StatusRuntimeException) {
                            reconnectAnalyticsReportStream();
                        }
                    }

                    @Override
                    public void onCompleted() {
                        // Nothing to do here
                    }
                }
        );
        initAutoReportAnalyticsReport();
    }


    private void sendAnalyticsReport(OcsgwAnalyticsReport report) {
        if (report != null) {
            ocsgwAnalyticsReportStream.onNext(report);
        }
    }

    private void reconnectAnalyticsReportStream() {
        LOG.debug("reconnectAnalyticsReportStream called");

        if (initAnalyticsFuture != null) {
            initAnalyticsFuture.cancel(true);
        }

        LOG.debug("Schedule new Callable initAnalyticsRequest");
        initAnalyticsFuture = executorService.schedule((Callable<Object>) () -> {
                    initAnalyticsRequestStream();
                    return "Called!";
                },
                5,
                TimeUnit.SECONDS);
    }

    private void initAutoReportAnalyticsReport() {

        if (sendAnalyticsFuture == null) {
            sendAnalyticsFuture = executorService.scheduleAtFixedRate(() -> {
                        sendAnalyticsReport(protobufDataSource.getAnalyticsReport());
                    },
                    0,
                    5,
                    TimeUnit.SECONDS);
        }
    }
}