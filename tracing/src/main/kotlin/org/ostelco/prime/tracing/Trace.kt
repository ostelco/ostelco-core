package org.ostelco.prime.tracing

import io.opencensus.common.Scope
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter
import io.opencensus.trace.Tracing
import io.opencensus.trace.samplers.Samplers

class TraceImpl : Trace by TraceSingleton

object TraceSingleton : Trace {

    private val tracer = Tracing.getTracer()

    // FIXME vihang: replace with Rate-limiting sampler before getting any serious load.
    // https://opencensus.io/tracing/sampling/ratelimited/
    private val sampler = Samplers.neverSample()

    fun init() {
        StackdriverTraceExporter.createAndRegister(
                StackdriverTraceConfiguration
                        .builder()
                        .build()
        )
        val traceConfig = Tracing.getTraceConfig()
        val activeTraceParams = traceConfig.activeTraceParams
        traceConfig.updateActiveTraceParams(
                activeTraceParams
                        .toBuilder()
                        .setSampler(Samplers.neverSample())
                        .build()
        )
    }

    fun createScopedSpan(name: String): Scope {
        return tracer
                .spanBuilder(name)
                .setSampler(sampler)
                .startScopedSpan()
    }

    override fun <T> childSpan(name: String, work: () -> T): T {
        val childSpan = tracer
                .spanBuilderWithExplicitParent(name, Tracing.getTracer().currentSpan)
                .setSampler(sampler)
                .startSpan()
        try {
            return work()
        } finally {
            childSpan.end()
        }
    }
}