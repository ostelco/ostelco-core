package org.ostelco.ext.myinfo

import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm.A256GCM
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm.RSA_OAEP
import org.apache.cxf.rs.security.jose.jwe.JweUtils
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.*

object JweCompactUtils {

    fun encrypt(base64PublicKey: String, content: String): String {

        val publicKey = KeyFactory
                .getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(
                        Base64.getDecoder().decode(base64PublicKey)))

        return JweUtils.encrypt(
                publicKey,
                RSA_OAEP,
                A256GCM,
                content.toByteArray())
    }
}