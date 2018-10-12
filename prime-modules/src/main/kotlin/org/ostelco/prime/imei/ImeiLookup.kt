package org.ostelco.prime.imei

import arrow.core.Either
import org.ostelco.prime.imei.core.Imei
import org.ostelco.prime.imei.core.ImeiLookupError

interface ImeiLookup {
    fun getImeiInformation(imei: String) : Either<ImeiLookupError, Imei>
}
