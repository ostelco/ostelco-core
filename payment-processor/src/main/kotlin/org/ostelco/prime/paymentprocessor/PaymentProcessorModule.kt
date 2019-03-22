package org.ostelco.prime.paymentprocessor

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.cloud.NoCredentials
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.KeyFactory
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import com.google.cloud.http.HttpTransportOptions
import com.stripe.Stripe
import io.dropwizard.setup.Environment
import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.paymentprocessor.publishers.StripeEventPublisher
import org.ostelco.prime.paymentprocessor.resources.StripeWebhookResource
import org.ostelco.prime.paymentprocessor.subscribers.RecurringPaymentStripeEvent
import org.ostelco.prime.paymentprocessor.subscribers.ReportStripeEvent
import org.ostelco.prime.paymentprocessor.subscribers.StoreStripeEvent

@JsonTypeName("stripe-payment-processor")
class PaymentProcessorModule : PrimeModule {

    private val logger by getLogger()

    @JsonProperty("config")
    fun setConfig(config: PaymentProcessorConfig) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
        logger.info("PaymentProcessor init with $env")

        /* Stripe requires the use an API key.
           https://stripe.com/docs/keys */
        Stripe.apiKey = System.getenv("STRIPE_API_KEY")
                ?: throw Error("Missing environment variable STRIPE_API_KEY")

        /* Setup datastore. */
        StripeStore.datastore = getDatastore()
        StripeStore.keyFactory = StripeStore.datastore.newKeyFactory()
                .setKind(ConfigRegistry.config.kind)

        val jerseyEnv = env.jersey()

        /* APIs. */
        jerseyEnv.register(StripeWebhookResource())

        /* Stripe events reporting. */
        env.lifecycle().manage(StripeEventPublisher)
        env.lifecycle().manage(StoreStripeEvent())
        env.lifecycle().manage(ReportStripeEvent())
        env.lifecycle().manage(RecurringPaymentStripeEvent())
    }

    private fun getDatastore() =
            when (ConfigRegistry.config.storeType) {
                "inmemory-emulator" -> {
                    logger.info("Starting with in-memory datastore emulator")
                    val helper = LocalDatastoreHelper.create(1.0)
                    helper.start()
                    helper.options
                }
                "emulator" -> {
                    logger.info("Starting with datastore emulator")
                    DatastoreOptions.newBuilder()
                            .setHost(ConfigRegistry.config.hostport)
                            .setCredentials(NoCredentials.getInstance())
                            .setTransportOptions(HttpTransportOptions.newBuilder()
                                    .build())
                            .build()
                }
                else -> {
                    logger.info("Using GCP datastore instance")
                    if (!ConfigRegistry.config.namespace.isEmpty()) {
                        DatastoreOptions.newBuilder()
                                .setNamespace(ConfigRegistry.config.namespace)
                    } else {
                        DatastoreOptions.newBuilder()
                    }.build()
                }
            }.service
}

class PaymentProcessorConfig {
    @NotEmpty
    @JsonProperty("projectId")
    lateinit var projectId: String

    @NotEmpty
    @JsonProperty("stripeEventTopicId")
    lateinit var stripeEventTopicId: String

    @NotEmpty
    @JsonProperty("stripeEventStoreSubscriptionId")
    lateinit var stripeEventStoreSubscriptionId: String

    @NotEmpty
    @JsonProperty("stripeEventReportSubscriptionId")
    lateinit var stripeEventReportSubscriptionId: String

    @NotEmpty
    @JsonProperty("stripeEventRecurringPaymentSubscriptionId")
    lateinit var stripeEventRecurringPaymentSubscriptionId: String

    @NotEmpty
    @JsonProperty("storeType")
    lateinit var storeType: String

    @JsonProperty("storeNamespace")
    var namespace: String = "Stripe"

    @JsonProperty("storeTableName")
    var kind: String = "stripe-events"

    @JsonProperty("hostport")
    var hostport: String = "localhost:9090"
}

object ConfigRegistry {
    lateinit var config: PaymentProcessorConfig
}

object StripeStore {
    lateinit var datastore: Datastore
    lateinit var keyFactory: KeyFactory
}
