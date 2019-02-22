package org.ostelco.prime.storage.firebase

fun initFirebaseConfigRegistry() {
    FirebaseConfigRegistry.firebaseConfig = FirebaseConfig(
            configFile = "config/prime-service-account.json",
            rootPath = "test")
}