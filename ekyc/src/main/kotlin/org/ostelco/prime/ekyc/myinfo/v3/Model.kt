package org.ostelco.prime.ekyc.myinfo.v3

import com.fasterxml.jackson.annotation.JsonProperty

open class DataItem {
    var source: String = ""
    var classification: String = ""
    @JsonProperty("lastupdated") var lastUpdated: String = ""
    var unavailable: Boolean = false
}

class ValueDataItem(
        val value: String
) : DataItem()

data class PersonData(
        val name: ValueDataItem,
        @JsonProperty("dob") val dateOfBirth: ValueDataItem?,
        @JsonProperty("passexpirydate") val passExpiryDate: ValueDataItem?
)