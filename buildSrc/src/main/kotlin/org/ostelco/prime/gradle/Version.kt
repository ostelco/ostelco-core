package org.ostelco.prime.gradle

object Version {
  const val assertJ = "3.13.2"

  const val arrow = "0.8.2"

  const val beam = "2.15.0"
  const val byteBuddy = "1.10.1"
  const val csv = "1.7"
  const val cxf = "3.3.3"
  const val dockerComposeJunitRule = "1.3.0"
  const val dropwizard = "1.3.15"
  const val metrics = "4.1.0"
  const val firebase = "6.10.0"

  const val googleCloud = "1.91.2"
  const val googleCloudDataStore = "1.97.0"
  const val googleCloudLogging = "0.115.0-alpha"
  const val googleCloudPubSub = "1.97.0"
  const val googleCloudStorage = "1.97.0"

  const val gson = "2.8.6"
  const val grpc = "1.24.0"
  const val guava = "28.1-jre"
  const val jackson = "2.10.0"
  const val jacksonDatabind = "2.10.0"
  const val javaxActivation = "1.1.1"
  const val javaxActivationApi = "1.2.0"
  const val javaxAnnotation = "1.3.2"
  // Keeping it version 1.16.1 to be consistent with grpc via PubSub client lib
  // Keeping it version 1.16.1 to be consistent with netty via Firebase lib
  const val jaxb = "2.3.1"
  const val jdbi3 = "3.10.1"
  const val jjwt = "0.10.7"
  const val junit5 = "5.5.2"
  const val kotlin = "1.3.50"
  const val kotlinXCoroutines = "1.3.2"
  const val mockito = "3.1.0"
  const val mockitoKotlin = "2.2.0"
  const val neo4jDriver = "1.7.5"
  const val neo4j = "3.5.11"
  const val opencensus = "0.24.0"
  const val postgresql = "42.2.8"  // See comment in ./sim-administration/simmanager/build.gradle
  const val prometheusDropwizard = "2.2.0"
  const val protoc = "3.10.0"
  const val slf4j = "1.7.28"
  // IMPORTANT: When Stripe SDK library version is updated, check if the Stripe API version has changed.
  // If so, then update API version in Stripe Web Console for callback Webhooks.
  const val stripe = "13.1.0"
  const val swagger = "2.0.10"
  const val swaggerCodegen = "2.4.9"
  const val testcontainers = "1.12.2"
  const val tink = "1.2.2"
  const val zxing = "3.4.0"
}