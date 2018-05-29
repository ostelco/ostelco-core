package org.ostelco.at

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.glassfish.jersey.client.JerseyClientBuilder
import org.glassfish.jersey.client.JerseyInvocation
import javax.ws.rs.client.Entity
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import kotlin.test.assertEquals

/**
 * Class to hold configuration which is set in DSL functions.
 */
class HttpRequest {
    lateinit var path: String
    var queryParams: Map<String, String> = emptyMap()
    var body: Any? = null
}

/**
 * DSL function for GET operation
 */
inline fun <reified T> get(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest()
    execute(request)
    val response = HttpClient().send(request.path, request.queryParams).get()
    assertEquals(200, response.status)
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * DSL function for POST operation
 */
inline fun <reified T> post(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest()
    execute(request)
    val response = HttpClient().send(request.path, request.queryParams)
            .post(Entity.entity(request.body ?: "", MediaType.APPLICATION_JSON_TYPE))
    assertEquals(201, response.status)
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * DSL function for PUT operation
 */
inline fun <reified T> put(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest()
    execute(request)
    val response = HttpClient().send(request.path, request.queryParams)
            .put(Entity.entity(request.body ?: "", MediaType.APPLICATION_JSON_TYPE))
    assertEquals(200, response.status)
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * Class which holds JerseyClient.
 * It is used by DSL functions to make actual HTTP Rest invocation.
 */
class HttpClient {

    private val jwtSigningKey = "jwt_secret"

    private val jerseyClient = JerseyClientBuilder.createClient()

    // url will be http://prime:8080 while running via docker-compose,
    // and will be http://localhost:9090 when running in IDE connecting to prime in docker-compose
    val url: String = "http://${System.getenv("PRIME_SOCKET") ?: "localhost:9090"}"

    private val token = Jwts.builder()
            .setClaims(mapOf("aud" to "http://ext-auth-provider:8080/userinfo"))
            .signWith(SignatureAlgorithm.HS512, jwtSigningKey)
            .compact()

    fun setup(path: String, queryParams: Map<String, String>, url: String): JerseyInvocation.Builder {
        var target = jerseyClient.target(url).path(path)
        queryParams.forEach { target = target.queryParam(it.key, it.value) }
        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer $token")
    }

    fun send(path: String, queryParams: Map<String, String>): JerseyInvocation.Builder {
        return setup(path, queryParams, url)
    }
}
