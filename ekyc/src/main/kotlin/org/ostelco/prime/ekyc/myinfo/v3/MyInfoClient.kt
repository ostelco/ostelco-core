package org.ostelco.prime.ekyc.myinfo.v3

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.jsonwebtoken.Jwts
import org.apache.cxf.rs.security.jose.jwe.JweCompactConsumer
import org.apache.cxf.rs.security.jose.jwe.JweUtils
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.ostelco.prime.ekyc.MyInfoData
import org.ostelco.prime.ekyc.MyInfoKycService
import org.ostelco.prime.ekyc.Registry.myInfoClient
import org.ostelco.prime.ekyc.myinfo.ExtendedCompressionCodecResolver
import org.ostelco.prime.ekyc.myinfo.HttpMethod
import org.ostelco.prime.ekyc.myinfo.HttpMethod.GET
import org.ostelco.prime.ekyc.myinfo.HttpMethod.POST
import org.ostelco.prime.ekyc.myinfo.TokenApiResponse
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.model.MyInfoConfig
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.inject.Named
import javax.ws.rs.core.MediaType
import kotlin.system.measureTimeMillis
import org.ostelco.prime.ekyc.ConfigRegistry.myInfoV3 as config

@Named("v3")
class MyInfoClient : MyInfoKycService by MyInfoClientSingleton

object MyInfoClientSingleton : MyInfoKycService {

    private val logger by getLogger()

    private val relaxedObjectMapper = jacksonObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false)

    override fun getConfig(): MyInfoConfig = MyInfoConfig(
            url = "${config.myInfoApiUri}/authorise" +
                    "?client_id=${config.myInfoApiClientId}" +
                    "&attributes=${config.myInfoPersonDataAttributes}" +
                    "&redirect_uri=${config.myInfoRedirectUri}")

    override fun getPersonData(authorisationCode: String): MyInfoData? {

        // Call /token API to get access_token
        val tokenApiResponse = getToken(authorisationCode = authorisationCode)
                ?.let { content ->
                    objectMapper.readValue(content, TokenApiResponse::class.java)
                }
                ?: return null

        // extract uin_fin out of "subject" of claims of access_token
        val claims = getClaims(tokenApiResponse.accessToken)
        val uinFin = claims.body.subject

        // Using access_token and uin_fin, call /person API to get Person Data
        val personDataString = getPersonData(
                uinFin = uinFin,
                accessToken = tokenApiResponse.accessToken)

        val personData = relaxedObjectMapper.readValue(personDataString, PersonData::class.java)

        return MyInfoData(
                uinFin = uinFin,
                personData = personDataString,
                birthDate = personData.dateOfBirth?.value?.let(LocalDate::parse),
                passExpiryDate = personData.passExpiryDate?.value?.let(LocalDate::parse)
        )
    }

    private fun getToken(authorisationCode: String): String? =
            sendSignedRequest(
                    httpMethod = POST,
                    path = "/token",
                    queryParams = mapOf(
                            "grant_type" to "authorization_code",
                            "code" to authorisationCode,
                            "redirect_uri" to config.myInfoRedirectUri,
                            "client_id" to config.myInfoApiClientId,
                            "client_secret" to config.myInfoApiClientSecret))

    private fun getClaims(jws: String) = Jwts.parser()
            .setCompressionCodecResolver(ExtendedCompressionCodecResolver)
            .setSigningKey(KeyFactory
                    .getInstance("RSA")
                    .generatePublic(X509EncodedKeySpec(Base64
                            .getDecoder()
                            .decode(config.myInfoServerPublicKey))))
            .parseClaimsJws(jws)

    private fun getPersonData(uinFin: String, accessToken: String): String? =
            sendSignedRequest(
                    httpMethod = GET,
                    path = "/person/$uinFin",
                    queryParams = mapOf(
                            "client_id" to config.myInfoApiClientId,
                            "attributes" to config.myInfoPersonDataAttributes),
                    accessToken = accessToken)

    /**
     * Ref: https://www.ndi-api.gov.sg/library/trusted-data/myinfo/tutorial3
     */
    private fun sendSignedRequest(
            httpMethod: HttpMethod,
            path: String,
            queryParams: Map<String, String>,
            accessToken: String? = null): String? {

        val queryParamsString = queryParams.entries.joinToString("&") { """${it.key}=${URLEncoder.encode(it.value, StandardCharsets.US_ASCII)}""" }

        val requestUrl = "${config.myInfoApiUri}$path"

        // Create HTTP request
        val request = when (httpMethod) {
            GET -> HttpGet("$requestUrl?$queryParamsString")
            POST -> HttpPost(requestUrl).also {
                it.entity = StringEntity(queryParamsString)
            }
        }

        if (config.myInfoApiEnableSecurity) {

            val nonce = SecureRandom.getInstance("SHA1PRNG").nextLong()
            val timestamp = Instant.now().toEpochMilli()

            // A) Construct the Authorisation Token Parameter
            val defaultAuthHeaders = mapOf(
                    "app_id" to config.myInfoApiClientId,
                    "timestamp" to "$timestamp",
                    "nonce" to "$nonce",
                    "signature_method" to "RS256")

            // B) Forming the Base String
            // Base String is a representation of the entire request (ensures message integrity)

            val baseStringParams = defaultAuthHeaders + queryParams

            // i) Normalize request parameters
            val baseParamString = baseStringParams.entries
                    .sortedBy { it.key }
                    .joinToString("&") { "${it.key}=${it.value}" }

            // ii) concatenate request elements (HTTP method + url + base string parameters)
            val baseString = "$httpMethod&$requestUrl&$baseParamString"

            // C) Signing Base String to get Digital Signature
            // Load pem file containing the x509 cert & private key & sign the base string with it to produce the Digital Signature
            val signature = Signature.getInstance("SHA256withRSA")
                    .also { sign ->
                        sign.initSign(KeyFactory
                                .getInstance("RSA")
                                .generatePrivate(PKCS8EncodedKeySpec(
                                        Base64.getDecoder().decode(config.myInfoClientPrivateKey))))
                    }
                    .also { sign -> sign.update(baseString.toByteArray()) }
                    .let(Signature::sign)
                    .let(Base64.getEncoder()::encodeToString)

            // D) Assembling the Authorization Header

            val authHeaders = defaultAuthHeaders +
                    mapOf("signature" to signature)

            var authHeaderString = "PKI_SIGN " +
                    authHeaders.entries
                            .joinToString(",") { """${it.key}="${it.value}"""" }

            if (accessToken != null) {
                authHeaderString = "$authHeaderString,Bearer $accessToken"
            }

            request.addHeader("Authorization", authHeaderString)

        } else if (accessToken != null) {
            request.addHeader("Authorization", "Bearer $accessToken")
        }

        request.addHeader("Cache-Control", "no-cache")
        request.addHeader("Accept", MediaType.APPLICATION_JSON)

        if (httpMethod == POST) {
            request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED)
        }

        var response: HttpResponse? = null

        val latency = measureTimeMillis {
            response = myInfoClient.execute(request)
        }

        logger.info("Latency is $latency ms for MyInfo $httpMethod")

        val statusCode  = response?.statusLine?.statusCode
        if (statusCode != 200) {
            logger.info("response: $httpMethod status: ${response?.statusLine}")
        }

        val content = response
                ?.entity
                ?.content
                ?.readAllBytes()
                ?.let { String(it) }

        if (content == null || statusCode != 200) {
            logger.info("$httpMethod Response content: $content")
            return null
        }

        if (config.myInfoApiEnableSecurity && httpMethod == GET) {
            // TODO vihang: Remove after initial testing is done.
            logger.info("jwe PersonData: {}", content)
            val jws = decodeJweCompact(content)
            // TODO vihang: Remove after initial testing is done.
            logger.info("jws PersonData: {}", jws)
            return getPersonDataFromJwsClaims(jws)
        }

        return content
    }

    private fun getPersonDataFromJwsClaims(jws: String): String {

        val correctedJws = jws
                // removing extra double-quotes
                .removePrefix("\"")
                .removeSuffix("\"")

        return asJson(getClaims(correctedJws).body)
    }

    private fun decodeJweCompact(jwePayload: String): String {

        val privateKey = KeyFactory
                .getInstance("RSA")
                .generatePrivate(PKCS8EncodedKeySpec(
                        Base64.getDecoder().decode(config.myInfoClientPrivateKey)))

        val jweHeaders = JweCompactConsumer(jwePayload).jweHeaders

        return String(JweUtils.decrypt(
                privateKey,
                jweHeaders.keyEncryptionAlgorithm,
                jweHeaders.contentEncryptionAlgorithm,
                jwePayload))
    }
}
