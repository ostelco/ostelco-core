import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(kotlin("stdlib-jdk8"))
  implementation("com.fasterxml.jackson.core:jackson-annotations:${Version.jackson}")
  implementation("com.google.cloud:google-cloud-datastore:${Version.googleCloudDataStore}")
  implementation("com.fasterxml.jackson.core:jackson-databind:${Version.jacksonDatabind}")

  // TODO vihang: this dependency is added only for @Exclude annotation for firebase
  implementation("com.google.firebase:firebase-admin:${Version.firebase}")
  implementation("org.slf4j:slf4j-api:${Version.slf4j}")
}