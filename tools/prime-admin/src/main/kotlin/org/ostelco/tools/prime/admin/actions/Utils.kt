package org.ostelco.tools.prime.admin.actions

import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.AdminDataSource

val formatJson = objectMapper.writerWithDefaultPrettyPrinter()::writeValueAsString

fun formatJson(json: String): String = objectMapper
        .readValue(json, Object::class.java)
        .let(formatJson)
