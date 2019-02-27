package org.ostelco.at.okhttp

import org.ostelco.at.common.Auth.generateAccessToken
import org.ostelco.at.common.url
import org.ostelco.prime.customer.ApiClient
import org.ostelco.prime.customer.api.DefaultApi

object ClientFactory {

    fun clientForSubject(subject: String): DefaultApi {
        val apiClient = ApiClient().setBasePath(url)
        apiClient.connectTimeout = 0
        apiClient.readTimeout = 0
        apiClient.writeTimeout = 0
        apiClient.setAccessToken(generateAccessToken(email = subject))
        return DefaultApi(apiClient)
    }
}