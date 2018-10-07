package org.ostelco.prime.slack

import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils
import org.ostelco.prime.getLogger

/**
 * Simple HttpClient for Slack
 */
class SlackWebHookClient(
        private val webHookUri: String,
        private val httpClient: CloseableHttpClient) {

    private val logger by getLogger()

    fun post(body: String) {
        val entity = EntityBuilder.create().apply { this.text = body }.build()
        val request = HttpPost(webHookUri).apply { this.entity = entity }
        val response = httpClient.execute(request)
        val responseText = EntityUtils.toString(response.entity)
        if (responseText != "ok") {
            logger.error("Failed to send messages to slack. Reason: {}", responseText)
        }
    }
}