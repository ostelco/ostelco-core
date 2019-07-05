package org.ostelco.prime.auth.firebase

import org.junit.Ignore
import org.junit.Test

class FirebaseAuthUtilTest {

    @Ignore
    @Test
    fun `test - createCustomToken`() {
        FirebaseAuthUtil.initUsingServiceAccount("../prime/config/prime-service-account.json")

        val token = FirebaseAuthUtil.createCustomToken(uid = "foo@bar.com")

        println(token)
    }
}