package org.ostelco.prime.sim

import arrow.core.Either
import org.ostelco.prime.model.SimEntry

interface SimManager {
    fun allocateNextEsimProfile(hlr: String, phoneType: String) : Either<String, SimEntry>
}