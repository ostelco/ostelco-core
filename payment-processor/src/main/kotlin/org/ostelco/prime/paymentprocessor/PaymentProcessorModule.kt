package org.ostelco.prime.paymentprocessor

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.dropwizard.setup.Environment
import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.prime.module.PrimeModule
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

@JsonTypeName("stripe-payment-processor")
class PaymentProcessorModule : PrimeModule {

    @JsonProperty("config")
    fun setConfig(config: PaymentProcessorConfig) {
    }

    @Override
    fun init(config: Environment) {
    }
}

class PaymentProcessorConfig {

    @NotEmpty
    @JsonProperty("something") // XXX Replace with Stripe config parameters
    lateinit var databaseName: String

    @NotEmpty
    @JsonProperty("somethingelse")  // XXX Replace with Stripe config parameters
    lateinit var configFile: String
}