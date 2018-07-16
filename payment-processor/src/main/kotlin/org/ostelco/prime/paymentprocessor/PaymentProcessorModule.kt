package org.ostelco.prime.paymentprocessor

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.prime.module.PrimeModule
import java.io.File
import com.stripe.Stripe

@JsonTypeName("stripe-payment-processor")
class PaymentProcessorModule : PrimeModule {


    @JsonProperty("config")
    fun setConfig(config: PaymentProcessorConfig) {
        val secretKey = File(config.configFile)?.readText(Charsets.UTF_8)
        Stripe.apiKey = secretKey
    }

    override fun init(env: Environment) {
        println("PaymentProcessor init with ${env}")
    }
}

class PaymentProcessorConfig {

    @NotEmpty
    @JsonProperty("configFile")
    lateinit var configFile: String
}