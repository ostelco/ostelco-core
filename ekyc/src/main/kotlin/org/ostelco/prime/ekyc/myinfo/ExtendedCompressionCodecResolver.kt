package org.ostelco.prime.ekyc.myinfo

import io.jsonwebtoken.CompressionCodec
import io.jsonwebtoken.Header
import io.jsonwebtoken.impl.compression.DefaultCompressionCodecResolver

/**
 * To handle `NONE` value for `zip` header in JWT.
 */
object ExtendedCompressionCodecResolver : DefaultCompressionCodecResolver() {

    override fun resolveCompressionCodec(header: Header<*>?): CompressionCodec? {

        if (header?.getCompressionAlgorithm() == "NONE") {
            return null
        }

        return super.resolveCompressionCodec(header)
    }
}