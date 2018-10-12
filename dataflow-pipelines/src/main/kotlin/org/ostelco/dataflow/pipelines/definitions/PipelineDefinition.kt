package org.ostelco.dataflow.pipelines.definitions

import org.apache.beam.sdk.Pipeline
import org.ostelco.dataflow.pipelines.ConsumptionPipelineOptions

interface PipelineDefinition {
    fun define(pipeline: Pipeline, options: ConsumptionPipelineOptions)
}