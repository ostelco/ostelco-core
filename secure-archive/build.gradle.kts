import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))
  
  implementation("com.fasterxml.jackson.core:jackson-databind:${Version.jacksonDatabind}")

  implementation("io.dropwizard:dropwizard-client:${Version.dropwizard}")
  implementation("com.google.cloud:google-cloud-datastore:${Version.googleCloudDataStore}")
  implementation("com.google.cloud:google-cloud-storage:${Version.googleCloudStorage}")
  implementation("com.google.crypto.tink:tink:${Version.tink}")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))
}

apply(from = "../gradle/jacoco.gradle")