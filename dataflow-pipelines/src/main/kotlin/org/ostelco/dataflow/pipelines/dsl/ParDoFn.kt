package org.ostelco.dataflow.pipelines.dsl

import org.apache.beam.sdk.transforms.DoFn
import org.apache.beam.sdk.transforms.ParDo

object ParDoFn {
    fun <InputT, OutputT> transform(process:(input: InputT) -> OutputT): ParDo.SingleOutput<InputT, OutputT> {
        return ParDo.of(object : DoFn<InputT, OutputT>() {
            @ProcessElement
            fun processElement(c: ProcessContext) {
                c.output(process(c.element()))
            }
        })
    }
}