package org.ostelco.prime.arrow

import arrow.core.Either
import arrow.core.Option
import arrow.core.orElse

fun <A> Option<A>.ifSuccessThen(next: () -> Option<A>): Option<A> = this.orElse(next)
fun <L, R> Option<L>.swapToEither(right: () -> R): Either<L, R> = this.toEither(right).swap()
fun <L, R> Either<L, R>.ifSuccessThen(next: () -> Either<L, R>): Either<L, R> = this.fold({ Either.left(it) }, { next() })
