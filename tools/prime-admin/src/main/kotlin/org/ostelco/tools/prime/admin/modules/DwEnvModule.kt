package org.ostelco.tools.prime.admin.modules

import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.client.HttpClientBuilder
import io.dropwizard.client.HttpClientConfiguration
import io.dropwizard.setup.Environment
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.StringEntity
import org.ostelco.prime.module.PrimeModule
import javax.ws.rs.core.MediaType

/**
 * Prime Module to get access to Dropwizard's Environment so that Prime Application can be shutdown gracefully.
 *
 */
@JsonTypeName("env")
class DwEnvModule : PrimeModule {

    val httpClient: HttpClientConfiguration = HttpClientConfiguration()

    override fun init(env: Environment) {
        Companion.env = env

        val httpClient = HttpClientBuilder(env)
                .using(this.httpClient)
                .build("SIM inventory")

        val request = RequestBuilder.post()
                .setUri("https://mconnect-es2-005.oberthur.net:1032/gsma/rsp2/es2plus/getProfileStatus") // prod
                // .setUri("https://mconnect-es2-005.staging.oberthur.net:1034/gsma/rsp2/es2plus/getProfileStatus") // dev
                .setHeader("User-Agent", "gsma-rsp-lpad")
                .setHeader("X-Admin-Protocol", "gsma/rsp/v2.0.0")
                .setHeader("Content-Type", MediaType.APPLICATION_JSON)
                .setEntity(StringEntity("{}"))
                .build()

        val response = httpClient.execute(request)

        println(response.statusLine)
        println(response.statusLine.statusCode)

    }

    companion object {
        lateinit var env: Environment
    }
}