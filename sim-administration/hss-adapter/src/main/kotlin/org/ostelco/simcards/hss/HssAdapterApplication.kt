package org.ostelco.simcards.hss

import com.codahale.metrics.health.HealthCheck
import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.lifecycle.Managed
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.simcards.admin.ConfigRegistry
import org.ostelco.simcards.admin.HssConfig
import org.ostelco.simcards.admin.mapRight
import org.ostelco.simcards.hss.profilevendors.api.ActivationRequest
import org.ostelco.simcards.hss.profilevendors.api.HssServiceGrpc.HssServiceImplBase
import org.ostelco.simcards.hss.profilevendors.api.HssServiceResponse
import org.ostelco.simcards.hss.profilevendors.api.SuspensionRequest
import javax.validation.Valid
import javax.validation.constraints.NotNull


fun main(args: Array<String>) = HssAdapterApplication().run(*args)

/**
 * The sim  manager will have to interface to many different Home Subscriber Module
 * instances.  Many of these will rely on proprietary libraries to interface to the
 * HSS.  We strongly believe that the majority of the Ostelco project's source code
 * should be open sourced, but it is impossible to open source something that isn't ours,
 * so we can't open source HSS libraries, and we won't.
 *
 * Instead we'll do the next best thing: We'll make it simple to create adapters
 * for these libraries and make them available to the ostelco core.
 *
 * Our strategy is to make a service, implemented by the HssAdapterApplication, that
 * will be available as an external executable, via rest  (or possibly gRPC,  not decided
 * at the time this documentation is  being written).  The "simmanager" module of the open
 * source Prime component will then connect to the hss profilevendors and make requests for
 * activation/suspension/deletion.
 *
 * This component is written in the open source project, and it contains a non-proprietary
 * implementation of a simple HSS interface.   We provide this as a template so that when
 * proprietary code is added to this application, it can be done in the same way as the
 * simple non-proprietary implementation was added.  You are however expected to do that,
 * and make your service, deploy it separately and tell the prime component where it is
 * (typically using kubernetes service lookup or something similar).
 */
class HssAdapterApplication : Application<HssAdapterApplicationConfiguration>() {

    override fun getName(): String {
        return "HSS adapter service"
    }

    override fun initialize(bootstrap: Bootstrap<HssAdapterApplicationConfiguration>?) {
        // nothing to do yet
    }

    override fun run(configuration: HssAdapterApplicationConfiguration,
                     env: Environment) {

        val httpClient = HttpClientBuilder(env)
                .using(ConfigRegistry.config.httpClient)
                .build("${getName()} http client")
        val jerseyEnv = env.jersey()


        /**
         * TODO: Add a couple of resources that tells the story about the
         *    adapters that are serving here, and which requests they are
         *    getting.
         */


        val myHssService = ManagedHssService(
                port = 9000,
                env = env,
                httpClient = httpClient,
                configuration = configuration.hssVendors)

        env.lifecycle().manage(myHssService)

        // This dispatcher  is what we will use to handle the incoming
        // requests.  it will essentially do all the work.
        // When it has been proven to work, we will make it something that can
        // be built in a separate reposítory, preferably using a library mechanism.
    }
}


class ManagedGrpcService (private val port: Int,
                          private val service: io.grpc.BindableService ) : Managed {

    private var server: Server

    init {
        this.server = ServerBuilder.forPort(port)
                .addService(service)
                .build()
    }


    @Throws(Exception::class)
    override fun start() {
        // Start the server
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

    val managedService: ManagedGrpcService

    init {
        val adapters = mutableSetOf<HssAdapter>()

        for (config in configuration) {
            // Only a simple profilevendors added here, ut this is the extension point where we will
            // add other, proprietary adapters eventually.
            adapters.add(SimpleHssAdapter(name = config.name, httpClient = httpClient, config = config))
        }

        val dispatcher = DirectHssDispatcher(adapters = adapters,
                healthCheckRegistrar = object : HealthCheckRegistrar {
                    override fun registerHealthCheck(name: String, healthCheck: HealthCheck) {
                        env.healthChecks().register(name, healthCheck)
                    }
                })

        val hssService = HssServiceImpl(dispatcher)

        this.managedService = ManagedGrpcService(port = port, service = hssService)

    }

    @Throws(Exception::class)
    override fun start() {
        managedService.start()
    }

    @Throws(Exception::class)
    override fun stop() {
        managedService.stop()
    }
}


class HssServiceImpl(private val hssDispatcher: HssDispatcher) : HssServiceImplBase() {


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


class HssAdapterApplicationConfiguration : Configuration() {
    @Valid
    @NotNull
    @JsonProperty("hlrs")
    lateinit var hssVendors: List<HssConfig>
}