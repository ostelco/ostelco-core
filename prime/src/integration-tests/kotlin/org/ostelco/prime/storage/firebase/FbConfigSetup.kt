package org.ostelco.prime.storage.firebase

fun initFirebaseConfigRegistry() {
    val firebaseConfig = FirebaseConfig()
    firebaseConfig.databaseName = "pantel-tests"
    firebaseConfig.configFile = "src/integration-tests/resources/pantel-tests.json"
    FirebaseConfigRegistry.firebaseConfig = firebaseConfig
}