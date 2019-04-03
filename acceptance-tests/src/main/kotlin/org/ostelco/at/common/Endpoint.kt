package org.ostelco.at.common

// url will be http://prime:8080 while running via docker-compose,
// and will be http://localhost:9090 when running in IDE connecting to prime in docker-compose
val url: String = "http://${System.getenv("PRIME_SOCKET") ?: "localhost:9090"}"

val ocsSocket = System.getenv("OCS_SOCKET") ?: "localhost:8082"

val pubSubEmulatorHost = System.getenv("PUBSUB_EMULATOR_HOST") ?: "localhost:8085"
