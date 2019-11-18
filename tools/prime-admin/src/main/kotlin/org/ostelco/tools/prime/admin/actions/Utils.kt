package org.ostelco.tools.prime.admin.actions

import arrow.core.Either
import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.storage.StoreError

val formatJson = objectMapper.writerWithDefaultPrettyPrinter()::writeValueAsString

fun formatJson(json: String): String = objectMapper
        .readValue(json, Object::class.java)
        .let(formatJson)

fun <L, R> Either<L, R>.printLeft() = this.mapLeft { left ->
    if (left is StoreError) {
        println(left.message)
    } else {
        println(left)
    }
    left
}

fun <L, R> Either<L, R>.print() = this.fold(
        { left ->
            println(left)
            this
        },
        { right ->
            println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(right))
            this
        }
)