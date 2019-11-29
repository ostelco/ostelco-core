package org.ostelco.simcards.profilevendors

import com.fasterxml.jackson.annotation.JsonProperty

// TODO:  Why on earth is the json property set to "metricName"? It makes no sense.
//        Fix it, but understand what it means.
data class ProfileVendorAdapterDatum(
        @JsonProperty("id") val id: Long,
        @JsonProperty("metricName") val name: String)

