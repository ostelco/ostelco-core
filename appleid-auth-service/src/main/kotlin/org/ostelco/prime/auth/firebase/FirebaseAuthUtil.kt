package org.ostelco.prime.auth.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.ostelco.common.firebasex.usingCredentialsFile

object FirebaseAuthUtil {

    fun initUsingServiceAccount(firebaseServiceAccount: String) {
        val options = FirebaseOptions.Builder()
                .usingCredentialsFile(firebaseServiceAccount)
                .build()

        FirebaseApp.initializeApp(options)
    }

    fun createCustomToken(uid: String): String = FirebaseAuth
            .getInstance()
            .createCustomTokenAsync(
                    uid,
                    mapOf("apple" to
                            mapOf(
                                    "identity" to uid,
                                    "type" to "APPLE_ID",
                                    "provider" to "apple.com"
                            )
                    )
            )
            .get()
}