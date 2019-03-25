package org.ostelco.prime.ekyc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.setup.Environment
import org.apache.http.client.HttpClient
import org.ostelco.prime.ekyc.Registry.myInfoClient
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("kyc")
class KycModule : PrimeModule {

    @JsonProperty
    fun setConfig(config: Config) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
        myInfoClient = HttpClientBuilder(env).build("MyInfoClient")
    }
}

data class Config(
        val myInfoApiUri: String,
        val myInfoApiClientId: String,
        val myInfoApiClientSecret: String,
        val myInfoApiEnableSecurity: Boolean = true,
        val myInfoApiRealm: String,
        val myInfoRedirectUri: String,
        val myInfoServerPublicKey: String,
        val myInfoClientPrivateKey: String,
        val myInfoPersonDataAttributes: String = "name,sex,dob,residentialstatus,nationality,mobileno,email,regadd")

object ConfigRegistry {
    lateinit var config: Config
}

object Registry {
    lateinit var myInfoClient: HttpClient;
}