package org.ostelco.at.jersey

import org.glassfish.jersey.client.JerseyClientBuilder
import org.glassfish.jersey.client.JerseyInvocation
import org.ostelco.at.common.accessToken
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
}

/**
 * DSL function for GET operation
 */
inline fun <reified T> get(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest()
    execute(request)
    val response = HttpClient.send(request.path, request.queryParams).get()
    assertEquals(200, response.status)
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * DSL function for POST operation
 */
inline fun <reified T> post(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest()
    execute(request)
    val response = HttpClient.send(request.path, request.queryParams)
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
    val response = HttpClient.send(request.path, request.queryParams)
            .put(Entity.entity(request.body ?: "", MediaType.APPLICATION_JSON_TYPE))
    assertEquals(200, response.status)
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * Class which holds JerseyClient.
 * It is used by DSL functions to make actual HTTP Rest invocation.
 */
object HttpClient {

    private val jerseyClient = JerseyClientBuilder.createClient()

    private fun setup(path: String, queryParams: Map<String, String>, url: String): JerseyInvocation.Builder {
        var target = jerseyClient.target(url).path(path)
        queryParams.forEach { target = target.queryParam(it.key, it.value) }
        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer $accessToken")
    }

    fun send(path: String, queryParams: Map<String, String>): JerseyInvocation.Builder {
        return setup(path, queryParams, url)
    }
}
