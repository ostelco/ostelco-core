package org.ostelco.prime.storage.graph

object StatusFlags {

    fun bitMapStatusFlags(vararg statusFlags: StatusFlag): Int = statusFlags.fold(0) { accumulator, statusFlag ->
        accumulator.or(power2(bitPosition(statusFlag)))
    }

    private fun bitPosition(statusFlag: StatusFlag) = statusFlag.ordinal

    private fun power2(power: Int): Int = if (power == 0) { 1 } else { 2 * power2(power - 1) }
}