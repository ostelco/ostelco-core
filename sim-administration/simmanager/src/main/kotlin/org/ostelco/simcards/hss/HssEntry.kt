package org.ostelco.simcards.hss


import com.fasterxml.jackson.annotation.JsonProperty

/**
 * This is a datum that is stored in a database.
 *
 * When a VLR asks the HLR for the an authentication triplet, then the
 * HLR will know that it should give an answer.
 *
 * id - is a database internal identifier.
 * metricName - is an unique instance of  HLR reference.
 */
data class HssEntry(
        @JsonProperty("id") val id: Long,
        @JsonProperty("metricName") val name: String)

