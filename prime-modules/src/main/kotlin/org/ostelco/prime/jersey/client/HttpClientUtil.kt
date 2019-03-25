package org.ostelco.prime.jersey.client

import org.glassfish.jersey.client.JerseyClientBuilder
import org.glassfish.jersey.client.JerseyInvocation
import org.glassfish.jersey.client.JerseyWebTarget
import org.ostelco.prime.getLogger
import org.ostelco.prime.jersey.client.HttpClient.logger
import javax.ws.rs.client.Entity
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedHashMap

/**
 * Class to hold configuration which is set in DSL functions.
 */
class HttpRequest {
    lateinit var target: String
    lateinit var path: String
    var headerParams: Map<String, List<String>> = emptyMap()
    var queryParams: Map<String, String> = emptyMap()
    var body: Any? = null
}

/**
 * DSL function for GET operation
 */
inline fun <reified T> get(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.target, request.path, request.queryParams, request.headerParams).get()
    if (200 != response.status) {
        logger.warn(response.readEntity(String::class.java))
    }
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * DSL function for POST operation
 */
inline fun <reified T> post(expectedResultCode: Int = 201, dataType: MediaType = MediaType.APPLICATION_JSON_TYPE, execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.target, request.path, request.queryParams, request.headerParams)
            .post(Entity.entity(request.body ?: "", dataType))
    if (expectedResultCode != response.status) {
        logger.warn(response.readEntity(String::class.java))
    }
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * DSL function for PUT operation
 */
inline fun <reified T> put(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.target, request.path, request.queryParams, request.headerParams)
            .put(Entity.entity(request.body ?: "", MediaType.APPLICATION_JSON_TYPE))
    if (200 != response.status) {
        logger.warn(response.readEntity(String::class.java))
    }
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * DSL function for DELETE operation
 */
inline fun <reified T> delete(execute: HttpRequest.() -> Unit): T {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.target, request.path, request.queryParams, request.headerParams)
            .delete()
    if (200 != response.status) {
        logger.warn(response.readEntity(String::class.java))
    }
    return response.readEntity(object : GenericType<T>() {})
}

/**
 * Class which holds JerseyClient.
 * It is used by DSL functions to make actual HTTP Rest invocation.
 */
object HttpClient {

    private val jerseyClient = JerseyClientBuilder.createClient()

    val logger by getLogger()

    fun send(
            target: String,
            path: String,
            queryParams: Map<String, String>,
            headerParams: Map<String, List<String>>): JerseyInvocation.Builder {

        var jerseyWebTarget: JerseyWebTarget = jerseyClient.target(target).path(path)
        queryParams.forEach { jerseyWebTarget = jerseyWebTarget.queryParam(it.key, it.value) }
        return jerseyWebTarget.request(MediaType.APPLICATION_JSON_TYPE)
                .headers(MultivaluedHashMap<String, Any>().apply { this.putAll(headerParams) })
    }
}
