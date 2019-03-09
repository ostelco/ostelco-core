package org.ostelco.prime.ekyc.myinfo

import io.jsonwebtoken.Jwts
import org.apache.cxf.rs.security.jose.jwe.JweCompactConsumer
import org.apache.cxf.rs.security.jose.jwe.JweUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.ostelco.prime.ekyc.ConfigRegistry.config
import org.ostelco.prime.ekyc.MyInfoKycService
import org.ostelco.prime.ekyc.Registry.myInfoClient
import org.ostelco.prime.ekyc.myinfo.HttpMethod.GET
import org.ostelco.prime.ekyc.myinfo.HttpMethod.POST
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.objectMapper
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.*
import javax.ws.rs.core.MediaType

class MyInfoClient : MyInfoKycService by MyInfoClientSingleton

object MyInfoClientSingleton : MyInfoKycService {

    private val logger by getLogger()

    override fun getPersonData(authorisationCode: String): String {

        // Call /token API to get access_token
        val tokenApiResponse = getToken(authorisationCode = authorisationCode)
                .let { content ->
                    objectMapper.readValue(content, TokenApiResponse::class.java)
                }

        // extract uin_fin out of "subject" of claims of access_token
        val claims = getClaims(tokenApiResponse.accessToken)
        val uinFin = claims.body.subject

        // Using access_token and uin_fin, call /person API to get Person Data
        return getPersonData(
                uinFin = uinFin,
                accessToken = tokenApiResponse.accessToken)
    }

    private fun getToken(authorisationCode: String): String =
            sendSignedRequest(
                    httpMethod = POST,
                    path = "/token",
                    queryParams = mapOf(
                            "grant_type" to "authorization_code",
                            "code" to authorisationCode,
                            "redirect_uri" to config.myInfoRedirectUri,
                            "client_id" to config.myInfoApiClientId,
                            "client_secret" to config.myInfoApiClientSecret))


    private fun getClaims(accessToken: String) = Jwts.parser()
            .setCompressionCodecResolver(ExtendedCompressionCodecResolver)
            .setSigningKey(KeyFactory
                    .getInstance("RSA")
                    .generatePublic(X509EncodedKeySpec(Base64
                            .getDecoder()
                            .decode(config.myInfoServerPublicKey))))
            .parseClaimsJws(accessToken)


    private fun getPersonData(uinFin: String, accessToken: String): String =
            sendSignedRequest(
                    httpMethod = GET,
                    path = "/person/$uinFin",
                    queryParams = mapOf(
                            "client_id" to config.myInfoApiClientId,
                            "attributes" to getPersonAttributes()),
                    accessToken = accessToken)


    private fun getPersonAttributes() = PersonApiAttributes
            .values()
            .joinToString(separator = ",") { it.label }

    /**
     * Ref: https://www.ndi-api.gov.sg/library/trusted-data/myinfo/tutorial3
     */
    private fun sendSignedRequest(
            httpMethod: HttpMethod,
            path: String,
            queryParams: Map<String, String>,
            accessToken: String? = null): String {

        val queryParamsString = queryParams.entries.joinToString("&") { """${it.key}=${URLEncoder.encode(it.value, StandardCharsets.US_ASCII)}""" }

        val requestUrl = "${config.myInfoApiUri}$path"

        // Create HTTP request
        val request = when (httpMethod) {
            GET -> HttpGet("$requestUrl?$queryParamsString")
            POST -> HttpPost(requestUrl).also {
                val body = queryParamsString // asJson(queryParams)
                logger.info("POST request body: $body")
                it.entity = StringEntity(body)
            }
        }

        if (config.myInfoApiEnableSecurity) {

            val nonce = SecureRandom.getInstance("SHA1PRNG").nextLong()
            val timestamp = Instant.now().toEpochMilli()

            // A) Construct the Authorisation Token Parameter
            val defaultAuthHeaders = mapOf(
                    "apex_l2_eg_timestamp" to "$timestamp",
                    "apex_l2_eg_nonce" to "$nonce",
                    "apex_l2_eg_app_id" to config.myInfoApiClientId,
                    "apex_l2_eg_signature_method" to "SHA256withRSA",
                    "apex_l2_eg_version" to "1.0")

            // B) Forming the Base String
            // Base String is a representation of the entire request (ensures message integrity)

            val baseStringParams = defaultAuthHeaders + queryParams

            // i) Normalize request parameters
            val baseParamString = baseStringParams.entries
                    .sortedBy { it.key }
                    .joinToString("&") { "${it.key}=${it.value}" }

            // ii) construct request URL ---> url is passed in to this function
            // NOTE: need to include the ".e." in order for the security authorisation header to work
            //myinfosgstg.api.gov.sg -> myinfosgstg.e.api.gov.sg

            val url = "${config.myInfoApiUri.toLowerCase().replace(".api.gov.sg", ".e.api.gov.sg")}$path"

            // iii) concatenate request elements (HTTP method + url + base string parameters)
            val baseString = "$httpMethod&$url&$baseParamString"

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

            val authHeaders = mapOf("realm" to config.myInfoApiRealm) +
                    defaultAuthHeaders +
                    mapOf("apex_l2_eg_signature" to signature)

            var authHeaderString = "apex_l2_eg " +
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
        request.addHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED)

        val response = myInfoClient.execute(request).also {
            if (it.statusLine.statusCode != 200) {
                logger.info("response: $httpMethod status: ${it.statusLine}")
            }
        }

        val content = response
                ?.entity
                ?.content
                ?.readAllBytes()
                ?.let { String(it) }
                ?.also {
                    logger.info("Response content: $it")
                }
                ?: ""

        if (config.myInfoApiEnableSecurity && httpMethod == GET) {
            return decodeJweCompact(content)
        }

        return content
    }

    internal fun decodeJweCompact(jwePayload: String): String {

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
