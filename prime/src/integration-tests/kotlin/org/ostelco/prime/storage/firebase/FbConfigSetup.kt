package org.ostelco.prime.storage.firebase

fun initFirebaseConfigRegistry() {
    val firebaseConfig = FirebaseConfig()
    firebaseConfig.databaseName = "pantel-2decb"
    firebaseConfig.configFile = "config/pantel-prod.json"
    firebaseConfig.rootPath = "test"
    FirebaseConfigRegistry.firebaseConfig = firebaseConfig
}