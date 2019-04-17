package org.ostelco.prime.sim

import arrow.core.Either
import org.ostelco.prime.model.SimEntry
import org.ostelco.prime.model.SimProfileStatus

interface SimManager {
    fun allocateNextEsimProfile(hlr: String, phoneType: String?) : Either<String, SimEntry>
    fun getSimProfileStatus(hlr: String, iccId:String) : Either<String, SimProfileStatus>
    fun getSimProfileStatusUpdates(onUpdate:(iccId:String, status: SimProfileStatus) -> Unit)
}