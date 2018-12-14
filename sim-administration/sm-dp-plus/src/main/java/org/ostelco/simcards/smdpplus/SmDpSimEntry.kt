package org.ostelco.simcards.smdpplus

class SmDpSimEntry (val iccid: String,
                    val imsi: String,
                    val profile: String) {
    var allocated: Boolean = false
    var eid: String? = null
}
