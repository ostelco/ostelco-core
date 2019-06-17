package org.ostelco.tools.prime.admin.actions

import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AdminDataSource
import kotlin.reflect.KFunction1

val formatJson: KFunction1<@ParameterName(name = "value") Any, String> = objectMapper.writerWithDefaultPrettyPrinter()::writeValueAsString

fun formatJson(json: String): String = objectMapper
        .readValue(json, Object::class.java)
        .let(formatJson)
