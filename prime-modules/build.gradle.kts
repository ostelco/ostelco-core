import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  api(kotlin("stdlib-jdk8"))
  api(kotlin("reflect"))
  api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.kotlinXCoroutines}")

  api("io.dropwizard:dropwizard-auth:${Version.dropwizard}")
  implementation("com.google.cloud:google-cloud-pubsub:${Version.googleCloudPubSub}")
  implementation("com.google.cloud:google-cloud-datastore:${Version.googleCloudDataStore}")

  api("com.fasterxml.jackson.module:jackson-module-kotlin:${Version.jackson}")

  api(project(":ocs-grpc-api"))
  api(project(":model"))

  api("io.dropwizard:dropwizard-core:${Version.dropwizard}")
  
  api("io.arrow-kt:arrow-core:${Version.arrow}")
  api("io.arrow-kt:arrow-typeclasses:${Version.arrow}")
  api("io.arrow-kt:arrow-instances-core:${Version.arrow}")
  api("io.arrow-kt:arrow-effects:${Version.arrow}")

  runtimeOnly("javax.xml.bind:jaxb-api:${Version.jaxb}")
  runtimeOnly("javax.activation:activation:${Version.javaxActivation}")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))
}

apply(from = "../gradle/jacoco.gradle.kts")