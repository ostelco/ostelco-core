package org.ostelco.simcards

import org.ostelco.simcards.LuhnChecksum.Companion.luhnComplete

/**
 *  MM = Constant (ISO 7812 Major Industry Identifier)
 *  CC = Country Code
 *  II = Issuer Identifier
 *  serialNumber = unique  positive number.
 */
class IccidBasis(val mm: Int = 89, val cc: Int = 1, val ii: Int = 0, val serialNumber: Int) {
    fun asIccid(): String {
        val protoIccid = "%02d%02d%02d%012d".format(mm, cc, ii, serialNumber)
        return luhnComplete(protoIccid)
    }
}
