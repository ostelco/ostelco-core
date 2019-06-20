package org.ostelco.tools.prime.admin.actions

import arrow.core.Either
import org.ostelco.prime.jsonmapper.objectMapper

val formatJson = objectMapper.writerWithDefaultPrettyPrinter()::writeValueAsString

fun formatJson(json: String): String = objectMapper
        .readValue(json, Object::class.java)
        .let(formatJson)

fun <L, R> Either<L, R>.printLeft() = this.mapLeft { left ->
    println(left)
    left
}

fun <L, R> Either<L, R>.print() = this.fold(
        { left ->
            println(left)
            this
        },
        { right ->
            println(right)
            this
        }
)