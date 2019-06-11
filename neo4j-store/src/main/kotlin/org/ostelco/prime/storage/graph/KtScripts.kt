package org.ostelco.prime.storage.graph

interface HssNameLookupService {
    fun getHssName(regionCode: String): String
}