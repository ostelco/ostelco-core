package org.ostelco.dataflow.pipelines.definitions

import org.apache.beam.sdk.Pipeline

interface PipelineDefinition {
    fun define(pipeline: Pipeline)
}