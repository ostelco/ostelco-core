package org.ostelco.dataflow.pipelines

import ch.qos.logback.classic.util.ContextInitializer
import org.apache.beam.runners.dataflow.DataflowRunner
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.options.PipelineOptions
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.ostelco.dataflow.pipelines.definitions.DataConsumptionPipelineDefinition
import org.ostelco.dataflow.pipelines.definitions.DummyPipelineDefinition
import org.ostelco.dataflow.pipelines.definitions.PipelineDefinition

enum class PipelineDefinitionRegistry(val pipelineDefinition: PipelineDefinition) {
    DATA_CONSUMPTION(DataConsumptionPipelineDefinition),
    DUMMY(DummyPipelineDefinition),
}

fun main(args: Array<String>) {
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "config/logback.xml")
    DeployPipeline().deploy(pipelineName = "DATA_CONSUMPTION")
}

class DeployPipeline {

    private fun parseOptions(): PipelineOptions {

        // may be we need to pass options via command-line args
        /*
    val options = PipelineOptionsFactory
            .fromArgs(
                    "--project=pantel-2decb",
                    "--runner=DataflowRunner",
                    "--stagingLocation=gs://data-traffic/staging/",
                    "--jobName=data-traffic")
            .withValidation()
            .create()
    */

        val options = PipelineOptionsFactory.`as`(DataflowPipelineOptions::class.java)
        options.jobName = "data-traffic"
        options.project = "pantel-2decb"
        options.stagingLocation = "gs://data-traffic/staging/"
        options.region = "europe-west1"
        options.runner = DataflowRunner::class.java
        options.isUpdate = true

        return options
    }

    fun deploy(pipelineName: String) {

        val options = parseOptions()

        PipelineDefinitionRegistry
                .valueOf(pipelineName)
                .apply {
                    Pipeline.create(options)
                            .apply { pipelineDefinition.define(this) }
                            .run()
                            .waitUntilFinish()
                }
    }
}