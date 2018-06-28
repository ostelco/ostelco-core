package org.ostelco.prime.appnotifier

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
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
}

class PaymentProcessorConfig {

    @NotEmpty
    @JsonProperty("something") // XXX Replace with Stripe config parameters
    lateinit var databaseName: String

    @NotEmpty
    @JsonProperty("somethingelse")  // XXX Replace with Stripe config parameters
    lateinit var configFile: String
}