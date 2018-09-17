package org.ostelco.prime.storage.firebase

fun initFirebaseConfigRegistry() {
    FirebaseConfigRegistry.firebaseConfig = FirebaseConfig(
            configFile = "config/pantel-prod.json",
            rootPath = "test")
}