package org.ostelco.simcards.hss

import com.codahale.metrics.health.HealthCheck
import io.dropwizard.lifecycle.Managed
import io.dropwizard.setup.Environment
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.simcards.admin.HssConfig
import org.ostelco.simcards.admin.mapRight
import org.ostelco.simcards.hss.profilevendors.api.ActivationRequest
import org.ostelco.simcards.hss.profilevendors.api.HssServiceGrpc
import org.ostelco.simcards.hss.profilevendors.api.HssServiceResponse
import org.ostelco.simcards.hss.profilevendors.api.SuspensionRequest


class ManagedGrpcService(private val port: Int,
                         private val service: io.grpc.BindableService) : Managed {

    private var server: Server

    init {
        this.server = ServerBuilder.forPort(port)
                .addService(service)
                .build()
    }

    @Throws(Exception::class)
    override fun start() {
        server.start()
    }

    @Throws(Exception::class)
    override fun stop() {
        server.awaitTermination()
    }
}

class ManagedHssService(
        private val configuration: List<HssConfig>,
        private val env: Environment,
        private val httpClient: CloseableHttpClient,
        private val port: Int) : Managed {

    private val managedGrpcService: ManagedGrpcService
    val dispatcher: HssDispatcher

    init {
        val adapters = mutableSetOf<HssAdapter>()

        this.dispatcher = DirectHssDispatcher(
                hssConfigs = configuration,
                httpClient = httpClient,
                healthCheckRegistrar = object : HealthCheckRegistrar {
                    override fun registerHealthCheck(name: String, healthCheck: HealthCheck) {
                        env.healthChecks().register(name, healthCheck)
                    }
                })

        val hssService = HssServiceImpl(dispatcher)

        this.managedGrpcService = ManagedGrpcService(port = port, service = hssService)
    }

    @Throws(Exception::class)
    override fun start() {
        managedGrpcService.start()
    }

    @Throws(Exception::class)
    override fun stop() {
        managedGrpcService.stop()
    }
}


class HssServiceImpl(private val hssDispatcher: HssDispatcher) : HssServiceGrpc.HssServiceImplBase() {


    override fun activate(request: ActivationRequest?, responseObserver: StreamObserver<HssServiceResponse>?) {

        if (request == null) return
        if (responseObserver == null) return

        hssDispatcher.activate(hssName = request.hss, iccid = request.iccid, msisdn = request.msisdn)
                .mapRight { responseObserver.onNext(HssServiceResponse.newBuilder().setSuccess(true).build()) }
                .mapLeft { responseObserver.onNext(HssServiceResponse.newBuilder().setSuccess(false).build()) }

    }

    override fun suspend(request: SuspensionRequest?, responseObserver: StreamObserver<HssServiceResponse>?) {
        if (request == null) return
        if (responseObserver == null) return

        hssDispatcher.suspend(hssName = request.hss, iccid = request.iccid)
                .mapRight { responseObserver.onNext(HssServiceResponse.newBuilder().setSuccess(true).build()) }
                .mapLeft { responseObserver.onNext(HssServiceResponse.newBuilder().setSuccess(false).build()) }
    }
}
