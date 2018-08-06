package org.ostelco.at.okhttp

import org.ostelco.at.common.generateAccessToken
import org.ostelco.at.common.url
import org.ostelco.prime.client.ApiClient
import org.ostelco.prime.client.api.DefaultApi

object ClientFactory {

    fun clientForSubject(subject: String): DefaultApi {
        val apiClient = ApiClient().setBasePath(url)
        apiClient.setAccessToken(generateAccessToken(subject = subject))
        return DefaultApi(apiClient)
    }
}