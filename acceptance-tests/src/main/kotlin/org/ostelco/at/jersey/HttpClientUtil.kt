package org.ostelco.at.jersey

import org.glassfish.jersey.client.JerseyClientBuilder
import org.glassfish.jersey.client.JerseyInvocation
import org.ostelco.at.common.Auth.generateAccessToken
import org.ostelco.at.common.url
import javax.ws.rs.client.Entity
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedHashMap
import kotlin.test.assertEquals

/**
 * Class to hold configuration which is set in DSL functions.
 */
class HttpRequest {
    lateinit var path: String
    var headerParams: Map<String, List<String>> = emptyMap()
    var queryParams: Map<String, String> = emptyMap()
    var body: Any? = null
    var email = "foo@bar.com"
}

/**
 * DSL function for GET operation
 */
inline fun <reified T> get(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.path, request.queryParams, request.headerParams, request.email).get()
    assertEquals(200, response.status) { response.readEntity(String::class.java) }
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * DSL function for POST operation
 */
inline fun <reified T> post(expectedResultCode: Int = 201, dataType: MediaType = MediaType.APPLICATION_JSON_TYPE, execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.path, request.queryParams, request.headerParams, request.email)
            .post(Entity.entity(request.body ?: "", dataType))
    assertEquals(expectedResultCode, response.status) { response.readEntity(String::class.java) }
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * DSL function for PUT operation
 */
inline fun <reified T> put(expectedResultCode: Int = 200, execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.path, request.queryParams, request.headerParams, request.email)
            .put(Entity.entity(request.body ?: "", MediaType.APPLICATION_JSON_TYPE))
    assertEquals(expectedResultCode, response.status) { response.readEntity(String::class.java) }
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * DSL function for DELETE operation
 */
inline fun <reified T> delete(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.path, request.queryParams, request.headerParams, request.email)
            .delete()
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

    private fun setup(
            path: String,
            queryParams: Map<String, String>,
            headerParams: Map<String, List<String>>,
            url: String,
            email: String): JerseyInvocation.Builder {

        var target = jerseyClient.target(url).path(path)
        queryParams.forEach { target = target.queryParam(it.key, it.value) }
        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .headers(MultivaluedHashMap<String, Any>().apply { this.putAll(headerParams) })
                .header("Authorization", "Bearer ${generateAccessToken(email)}")
    }

    fun send(path: String, queryParams: Map<String, String>, headerParams: Map<String, List<String>>, email: String): JerseyInvocation.Builder =
            setup(path, queryParams, headerParams, url, email)
}
