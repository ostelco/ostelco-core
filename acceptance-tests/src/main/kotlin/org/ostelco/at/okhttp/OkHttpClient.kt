package org.ostelco.at.okhttp

import org.ostelco.at.common.accessToken
import org.ostelco.at.common.url
import org.ostelco.prime.client.ApiClient
import org.ostelco.prime.client.api.DefaultApi

object OkHttpClient {

    private val apiClient = ApiClient()
            .setBasePath(url)

    init {
        apiClient.setAccessToken(accessToken)
    }

    val client = DefaultApi(apiClient)
}