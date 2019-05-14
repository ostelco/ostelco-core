package org.ostelco.simcards.admin

import arrow.core.Either


/**
 * TODO: 1. Document this method
 */
fun <L,R, R2> Either<L, R>.mapRight(f:(R)->R2): Either<L, R2> = this.map(f)