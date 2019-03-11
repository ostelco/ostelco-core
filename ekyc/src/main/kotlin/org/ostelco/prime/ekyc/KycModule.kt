package org.ostelco.prime.ekyc

import com.fasterxml.jackson.annotation.JsonProperty
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.setup.Environment
import org.apache.http.client.HttpClient
import org.ostelco.prime.ekyc.Registry.myInfoClient
import org.ostelco.prime.module.PrimeModule

class KycModule : PrimeModule {

    @JsonProperty
    fun setConfig(config: Config) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
        myInfoClient = HttpClientBuilder(env).build("MyInfoClient")
    }
}

/**
 * Ref: https://www.ndi-api.gov.sg/assets/lib/trusted-data/myinfo/specs/myinfo-kyc-v2.1.1.yaml.html#section/Environments
 *
 *  https://{ENV_DOMAIN_NAME}/{VERSION}/{RESOURCE}
 *
 * ENV_DOMAIN_NAME:
 *  - Sandbox/Dev: https://myinfosgstg.api.gov.sg/dev/
 *  - Staging: https://myinfosgstg.api.gov.sg/test/
 *  - Production: https://myinfosg.api.gov.sg/
 *
 * VERSION: `/v2`
 */
object TestConfig {
    const val myInfoApiUri: String = "https://myinfosgstg.api.gov.sg/test/v2"
    const val myInfoApiClientId: String = "STG2-MYINFO-SELF-TEST"
    const val myInfoApiClientSecret: String = "44d953c796cccebcec9bdc826852857ab412fbe2"
    const val myInfoRedirectUri: String = "http://localhost:3001/callback"
    const val myInfoApiRealm: String = "http://localhost:3001"
}

data class Config(
        val myInfoApiUri: String = TestConfig.myInfoApiUri,
        val myInfoApiClientId: String = TestConfig.myInfoApiClientId,
        val myInfoApiClientSecret: String = TestConfig.myInfoApiClientSecret,
        val myInfoApiEnableSecurity: Boolean = true,
        val myInfoApiRealm: String = TestConfig.myInfoApiRealm,
        val myInfoRedirectUri: String = TestConfig.myInfoRedirectUri,
        val myInfoServerPublicKey: String,
        val myInfoClientPrivateKey: String)



object ConfigRegistry {
    lateinit var config: Config
}

object Registry {
    lateinit var myInfoClient: HttpClient;
}