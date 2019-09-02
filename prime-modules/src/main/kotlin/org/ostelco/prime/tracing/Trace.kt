package org.ostelco.prime.tracing

interface Trace {
    fun <T> childSpan(name: String, work: () -> T) : T
}