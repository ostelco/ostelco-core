package org.ostelco.dataflow.pipelines

import ch.qos.logback.classic.util.ContextInitializer
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.options.Default
import org.apache.beam.sdk.options.Description
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.ostelco.dataflow.pipelines.definitions.DataConsumptionPipelineDefinition
import org.ostelco.dataflow.pipelines.definitions.DummyPipelineDefinition
import org.ostelco.dataflow.pipelines.definitions.PipelineDefinition


enum class PipelineDefinitionRegistry(val pipelineDefinition: PipelineDefinition) {
    DATA_CONSUMPTION(DataConsumptionPipelineDefinition),
    DUMMY(DummyPipelineDefinition),
}

interface ConsumptionPipelineOptions : DataflowPipelineOptions {
    @get:Description("Dataset name.")
    @get:Default.String("data_consumption")
    var dataset: String
    @get:Description("PubSub toipc name.")
    @get:Default.String("data-traffic")
    var pubsubTopic: String
}

fun main(args: Array<String>) {
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "config/logback.xml")
    DeployPipeline().deploy(
            pipelineName = "DATA_CONSUMPTION",
            args = args)
}

class DeployPipeline {

    private fun parseOptions(args: Array<String>): ConsumptionPipelineOptions {

        PipelineOptionsFactory.register(ConsumptionPipelineOptions::class.java)

        val options = PipelineOptionsFactory
                .fromArgs(*args)
                .withValidation()
                .`as`(ConsumptionPipelineOptions::class.java)

        println("${options.dataset}, ${options.pubsubTopic}, ${options.jobName}, ${options.isUpdate} ")

        return options
    }

    fun deploy(pipelineName: String, args: Array<String>) {

        val options = parseOptions(args)
        PipelineDefinitionRegistry
                .valueOf(pipelineName)
                .apply {
                    Pipeline.create(options)
                            .apply { pipelineDefinition.define(this, options) }
                            .run()
                            .waitUntilFinish()
                }
    }
}