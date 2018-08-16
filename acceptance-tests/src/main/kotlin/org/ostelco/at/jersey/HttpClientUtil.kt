package org.ostelco.at.jersey

import org.glassfish.jersey.client.JerseyClientBuilder
import org.glassfish.jersey.client.JerseyInvocation
import org.ostelco.at.common.Auth.generateAccessToken
import org.ostelco.at.common.url
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
    var subscriberId = "foo@bar.com"
}

/**
 * DSL function for GET operation
 */
inline fun <reified T> get(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.path, request.queryParams, request.subscriberId).get()
    assertEquals(200, response.status) { response.readEntity(String::class.java) }
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * DSL function for POST operation
 */
inline fun <reified T> post(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.path, request.queryParams, request.subscriberId)
            .post(Entity.entity(request.body ?: "", MediaType.APPLICATION_JSON_TYPE))
    assertEquals(201, response.status) { response.readEntity(String::class.java) }
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * DSL function for PUT operation
 */
inline fun <reified T> put(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.path, request.queryParams, request.subscriberId)
            .put(Entity.entity(request.body ?: "", MediaType.APPLICATION_JSON_TYPE))
    assertEquals(200, response.status) { response.readEntity(String::class.java) }
    return response.readEntity(object : GenericType<T>() {})
}

fun <T> assertEquals(expected: T, actual: T, lazyMessage: () -> String) {
    var message = ""
    if (expected != actual) {
        message = lazyMessage()
    }
    assertEquals(expected, actual, message)
}

/**
 * Class which holds JerseyClient.
 * It is used by DSL functions to make actual HTTP Rest invocation.
 */
object HttpClient {

    private val jerseyClient = JerseyClientBuilder.createClient()

    private fun setup(path: String, queryParams: Map<String, String>, url: String, subscriberId: String): JerseyInvocation.Builder {
        var target = jerseyClient.target(url).path(path)
        queryParams.forEach { target = target.queryParam(it.key, it.value) }
        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer ${generateAccessToken(subscriberId)}")
    }

    fun send(path: String, queryParams: Map<String, String>, subscriberId: String): JerseyInvocation.Builder =
            setup(path, queryParams, url, subscriberId)
}
