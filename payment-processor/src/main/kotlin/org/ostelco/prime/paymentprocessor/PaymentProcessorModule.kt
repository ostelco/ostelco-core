package org.ostelco.prime.paymentprocessor

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.stripe.Stripe
import io.dropwizard.setup.Environment
import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.prime.getLogger
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.notifications.NOTIFY_OPS_MARKER
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

        val API_VERSION = "2019-03-14"

        if (Stripe.API_VERSION != API_VERSION) {
            logger.warn(NOTIFY_OPS_MARKER, "Stripe API version is ${Stripe.API_VERSION} but expected ${API_VERSION}")
        }

        val jerseyEnv = env.jersey()

        /* APIs. */
        jerseyEnv.register(StripeWebhookResource())

        /* Stripe events reporting. */
        env.lifecycle().manage(StripeEventPublisher)
        env.lifecycle().manage(StoreStripeEvent())
        env.lifecycle().manage(ReportStripeEvent())
        env.lifecycle().manage(RecurringPaymentStripeEvent())
    }
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

    @JsonProperty("stripeEventStoreType")
    var stripeEventStoreType: String = "default"

    /* Same as 'table name' in other DBs. */
    @JsonProperty("stripeEventKind")
    var stripeEventKind: String = "stripe-events"

    /* Can be used to set 'namespace' in Datastore.
       Not used if set to an emtpy string. */
    @JsonProperty("namespace")
    var namespace: String = ""

    /* Only used by Datastore emulator. */
    @JsonProperty("hostport")
    var hostport: String = "localhost:9090"
}

object ConfigRegistry {
    lateinit var config: PaymentProcessorConfig
}
