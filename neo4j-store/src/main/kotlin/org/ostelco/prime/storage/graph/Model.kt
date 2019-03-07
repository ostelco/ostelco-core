package org.ostelco.prime.storage.graph

import org.ostelco.prime.model.HasId

data class Identity(
        override val id: String,
        val type: String) : HasId

data class Identifies(val provider: String)
