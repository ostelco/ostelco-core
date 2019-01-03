package org.ostelco.prime.ocs

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.hibernate.validator.constraints.NotEmpty
import org.ostelco.prime.module.PrimeModule
import org.ostelco.prime.ocs.consumption.OcsGrpcServer
import org.ostelco.prime.ocs.consumption.OcsGrpcService
import org.ostelco.prime.ocs.core.OnlineCharging

@JsonTypeName("ocs")
class OcsModule : PrimeModule {

    @JsonProperty
    fun setConfig(config: Config) {
        ConfigRegistry.config = config
    }

    override fun init(env: Environment) {
        env.lifecycle().manage(
                OcsGrpcServer(8082, OcsGrpcService(OnlineCharging)))
    }
}

class Config {
    @NotEmpty
    @JsonProperty("lowBalanceThreshold")
    var lowBalanceThreshold: Long = 0
}

object ConfigRegistry {
    lateinit var config: Config
}