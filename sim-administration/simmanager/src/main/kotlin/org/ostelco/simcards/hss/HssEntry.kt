package org.ostelco.simcards.hss


import arrow.core.Either
import arrow.core.Left
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.ostelco.prime.getLogger
import org.ostelco.prime.simmanager.AdapterError
import org.ostelco.prime.simmanager.NotUpdatedError
import org.ostelco.prime.simmanager.SimManagerError
import org.ostelco.simcards.admin.HssConfig
import javax.ws.rs.core.MediaType

/**
 * This is a datum that is stored in a database.
 *
 * When a VLR asks the HLR for the an authentication triplet, then the
 * HLR will know that it should give an answer.
 *
 * id - is a database internal identifier.
 * name - is an unique instance of  HLR reference.
 */
data class HssEntry(
        @JsonProperty("id") val id: Long,
        @JsonProperty("name") val name: String)

