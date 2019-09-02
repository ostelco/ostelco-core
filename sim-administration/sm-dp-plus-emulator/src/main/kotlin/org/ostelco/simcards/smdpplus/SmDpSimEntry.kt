package org.ostelco.simcards.smdpplus

import org.slf4j.LoggerFactory

class SmDpSimEntry (val iccid: String,
                    val imsi: String,
                    val profile: String,
                    var state: String = "AVAILABLE") {
    var allocated: Boolean = false
    var eid: String? = null
    var released: Boolean = false
    var confirmationCode: String? = null
    var machingId: String? = null
    var smdsAddress :String? = null


    private val log = LoggerFactory.getLogger(javaClass)

    fun clone(): SmDpSimEntry {
        return SmDpSimEntry(iccid = iccid, imsi=imsi, profile=profile, state = state)
    }

    fun setCurrentState(s: String) {
        log.info("Changing state if sim entry for iccid $iccid from ${this.state} to $s")
        this.state = s
    }
}
