package org.ostelco.prime.jersey.client

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.glassfish.jersey.client.JerseyClientBuilder
import org.glassfish.jersey.client.JerseyInvocation
import org.glassfish.jersey.client.JerseyWebTarget
import org.ostelco.prime.getLogger
import org.ostelco.prime.jsonmapper.objectMapper
import javax.ws.rs.client.Entity
import javax.ws.rs.core.Form
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
    var form: Form? = null
}

data class HttpErrorResponse<L>(
        val status: Int,
        val error: L
)

/**
 * DSL function for GET operation
 */
inline fun <reified L, reified R> get(execute: HttpRequest.() -> Unit): Either<HttpErrorResponse<L>, R> {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.target, request.path, request.queryParams, request.headerParams)
            .get()
    if (200 != response.status) {
        return HttpErrorResponse(status = response.status, error = objectMapper.readValue(response.readEntity(object : GenericType<String>() {}), L::class.java)).left()
    }
    return response.readEntity(object : GenericType<R>() {}).right()
}

/**
 * DSL function for POST operation
 */
inline fun <reified L, reified R> post(expectedResultCode: Int = 201, dataType: MediaType = MediaType.APPLICATION_JSON_TYPE, execute: HttpRequest.() -> Unit): Either<HttpErrorResponse<L>, R> {
    val request = HttpRequest().apply(execute)
    val invocation = HttpClient.send(request.target, request.path, request.queryParams, request.headerParams)
    val response = request.form?.let { form -> invocation.post(Entity.form(form)) }
                        ?: invocation.post(Entity.entity(request.body ?: "", dataType))
    if (expectedResultCode != response.status) {
        return HttpErrorResponse(status = response.status, error = objectMapper.readValue(response.readEntity(object : GenericType<String>() {}), L::class.java)).left()
    }
    return objectMapper.readValue(response.readEntity(object : GenericType<String>() {}), R::class.java).right()
}

/**
 * DSL function for PUT operation
 */
inline fun <reified L, reified R> put(execute: HttpRequest.() -> Unit): Either<HttpErrorResponse<L>, R> {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.target, request.path, request.queryParams, request.headerParams)
            .put(Entity.entity(request.body ?: "", MediaType.APPLICATION_JSON_TYPE))
    if (200 != response.status) {
        return HttpErrorResponse(status = response.status, error = objectMapper.readValue(response.readEntity(object : GenericType<String>() {}), L::class.java)).left()
    }
    return objectMapper.readValue(response.readEntity(object : GenericType<String>() {}), R::class.java).right()
}

/**
 * DSL function for DELETE operation
 */
inline fun <reified L, reified R> delete(execute: HttpRequest.() -> Unit): Either<HttpErrorResponse<L>, R> {
    val request = HttpRequest().apply(execute)
    val response = HttpClient.send(request.target, request.path, request.queryParams, request.headerParams)
            .delete()
    if (200 != response.status) {
        return HttpErrorResponse(status = response.status, error = objectMapper.readValue(response.readEntity(object : GenericType<String>() {}), L::class.java)).left()
    }
    return objectMapper.readValue(response.readEntity(object : GenericType<String>() {}), R::class.java).right()
}

/**
 * Class which holds JerseyClient.
 * It is used by DSL functions to make actual HTTP Rest invocation.
 */
object HttpClient {

    private val jerseyClient = JerseyClientBuilder
            .createClient()
            .register(objectMapper)

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
