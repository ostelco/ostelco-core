package org.ostelco.simcards.admin

import arrow.core.Either


/**
 * TODO: 1. Document this method
 *       2. Move it into a convenience libary for Arrow utilities.
 */
fun <L,R, R2> Either<L, R>.mapRight(f:(R)->R2): Either<L, R2> = this.map(f)