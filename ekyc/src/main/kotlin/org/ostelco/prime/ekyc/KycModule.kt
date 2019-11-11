package org.ostelco.prime.ekyc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.ostelco.prime.ekyc.Registry.myInfoClient
import org.ostelco.prime.module.PrimeModule

@JsonTypeName("kyc")
class KycModule : PrimeModule {

    @JsonProperty
    fun setConfig(config: Config) {
        ConfigRegistry.myInfoV3 = config.myInfoV3
    }

    override fun init(env: Environment) {
        val connManager = PoolingHttpClientConnectionManager()

        /* Defaults for httpclient:
             max-total = 20
             default-max-per-route = 2
           Sets these to higher values as this is too low. */
        /* TODO: Make this configurable or something - or maybe
                 just follow up on the todo below... */
        connManager.maxTotal = 1024
        connManager.defaultMaxPerRoute = 1024

        // TODO change this to Dropwizard's HttpClientBuilder with appropriate timeout values
        myInfoClient = HttpClientBuilder.create().setConnectionManager(connManager).build()
    }
}

data class Config(
        val myInfoV3: MyInfoV3Config
)

data class MyInfoV3Config(
        val myInfoApiUri: String,
        val myInfoApiClientId: String,
        val myInfoApiClientSecret: String,
        val myInfoApiEnableSecurity: Boolean = true,
        val myInfoRedirectUri: String,
        val myInfoServerPublicKey: String,
        val myInfoClientPrivateKey: String,
        val myInfoPersonDataAttributes: String = "name,dob,mailadd,regadd,passexpirydate,uinfin"
)

object ConfigRegistry {
    lateinit var myInfoV3: MyInfoV3Config
}

object Registry {
    lateinit var myInfoClient: HttpClient
}