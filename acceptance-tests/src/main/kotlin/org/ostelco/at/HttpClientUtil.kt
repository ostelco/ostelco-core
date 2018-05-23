package org.ostelco.at

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.glassfish.jersey.client.JerseyClientBuilder
import org.glassfish.jersey.client.JerseyInvocation
import javax.ws.rs.client.Entity
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.test.assertEquals

/**
 * Class to hold configuration which is set in DSL functions.
 */
class HttpRequest {
    lateinit var path: String
    var body: Any? = null
}

/**
 * DSL function for GET operation
 */
inline fun <reified T> get(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest()
    execute(request)
    return HttpClient().send(request.path).get(object : GenericType<T>(){})
}

/**
 * DSL function for POST operation
 */
fun post(execute: HttpRequest.() -> Unit): Response {
    val request = HttpRequest()
    execute(request)
    val response = HttpClient().send(request.path)
            .post(Entity.entity(request.body ?: "", MediaType.APPLICATION_JSON_TYPE))
    assertEquals(201, response.status)
    return response
}

/**
 * DSL function for PUT operation
 */
fun put(execute: HttpRequest.() -> Unit): Response {
    val request = HttpRequest()
    execute(request)
    val response = HttpClient().send(request.path)
            .put(Entity.entity(request.body ?: "", MediaType.APPLICATION_JSON_TYPE))
    assertEquals(200, response.status)
    return response
}

/**
 * Class which holds JerseyClient.
 * It is used by DSL functions to make actual HTTP Rest invocation.
 */
class HttpClient {

    private val namespace = "https://ostelco"
    private val key = "jwtsecret"

    private val jerseyClient = JerseyClientBuilder.createClient()

    // url will be http://prime:8080 while running via docker-compose,
    // and will be http://localhost:9090 when running in IDE connecting to prime in docker-compose
    val url: String = "http://${System.getenv("PRIME_SOCKET") ?: "localhost:9090"}"

    private val token = Jwts.builder()
            .setClaims(mapOf(Pair("$namespace/email", "foo@bar.com")))
            .signWith(SignatureAlgorithm.HS512, key)
            .compact()

    fun setup(path: String, url: String): JerseyInvocation.Builder {
        return jerseyClient.target(url)
                .path(path)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer $token")
    }

    fun send(path: String): JerseyInvocation.Builder {
        return setup(path, url)
    }
}
