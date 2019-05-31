package org.ostelco.tools.prime.admin.modules

import arrow.core.Either
import org.ostelco.prime.model.SimEntry
import org.ostelco.prime.model.SimProfileStatus
import org.ostelco.prime.model.SimProfileStatus.NOT_READY
import org.ostelco.prime.sim.SimManager

class DummySimManager : SimManager {

    override fun allocateNextEsimProfile(hlr: String, phoneType: String?): Either<String, SimEntry> {
        return Either.left("Service unavailable")
    }

    override fun getSimProfile(hlr: String, iccId: String): Either<String, SimEntry> {
        return Either.right(SimEntry(
                iccId = iccId,
                status = NOT_READY,
                eSimActivationCode = "unknown",
                msisdnList = emptyList()))
    }

    override fun getSimProfileStatusUpdates(onUpdate: (iccId: String, status: SimProfileStatus) -> Unit) {

    }
}