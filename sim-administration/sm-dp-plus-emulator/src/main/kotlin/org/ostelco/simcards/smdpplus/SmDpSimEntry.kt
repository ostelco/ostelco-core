package org.ostelco.simcards.smdpplus

class SmDpSimEntry (val iccid: String,
                    val imsi: String,
                    val profile: String) {
    var allocated: Boolean = false
    var eid: String? = null
    var released: Boolean = false
    var confirmationCode: String? = null
    var machingId: String? = null
    var smdsAddress :String? = null


    fun clone(): SmDpSimEntry {
        return SmDpSimEntry(iccid = iccid, imsi=imsi, profile=profile)
    }
}
