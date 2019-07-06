package org.ostelco.prime.auth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import io.dropwizard.setup.Environment
import org.ostelco.prime.auth.firebase.FirebaseAuthUtil
import org.ostelco.prime.auth.resources.AppleIdAuthResource
import org.ostelco.prime.module.PrimeModule
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

@JsonTypeName("apple-id-auth")
class AppleIdAuthModule : PrimeModule {

    @JsonProperty
    private lateinit var config: Config

    override fun init(env: Environment) {

        ConfigRegistry.config = InternalConfig(
                teamId = config.teamId,
                keyId = config.keyId,
                clientId = config.clientId,
                privateKey = KeyFactory
                        .getInstance("EC")
                        .generatePrivate(
                                PKCS8EncodedKeySpec(
                                        Base64.getDecoder().decode(config.privateKey)
                                )
                        )
        )

        FirebaseAuthUtil.initUsingServiceAccount(config.firebaseServiceAccount)

        env.jersey().register(AppleIdAuthResource())
    }
}

data class Config(
        val teamId: String,
        val keyId: String,
        val clientId: String,
        val privateKey: String,
        val firebaseServiceAccount: String
)

data class InternalConfig(
        val teamId: String,
        val keyId: String,
        val clientId: String,
        val privateKey: PrivateKey,
        val appleIdServiceUrl:String = "https://appleid.apple.com"
)

object ConfigRegistry {
    lateinit var config: InternalConfig
}