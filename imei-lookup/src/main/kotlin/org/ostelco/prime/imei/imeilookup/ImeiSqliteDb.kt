package org.ostelco.prime.imei.imeilookup

import arrow.core.Either
import org.ostelco.prime.getLogger
import org.ostelco.prime.imei.ImeiLookup
import org.ostelco.prime.imei.core.Imei
import org.ostelco.prime.imei.core.ImeiLookupError
import org.ostelco.prime.imei.core.ImeaiNotFoundError


/**
 *  SQLite implementation of the IMEI lookup service
 */
class ImeiSqliteDb : ImeiLookup by jdbcSingleton {

    object jdbcSingleton :  ImeiLookup {

        private val logger by getLogger()

        init {
            logger.info("Singleton created")
        }

        override fun getImeiInformation(imeisv: String): Either<ImeiLookupError, Imei> {
            return Either.left(ImeaiNotFoundError("Not implemented jet"))
        }
    }
}
