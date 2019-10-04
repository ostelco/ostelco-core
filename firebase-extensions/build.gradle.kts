import org.ostelco.prime.gradle.Version

plugins {
  kotlin("jvm")
  `java-library`
}

dependencies {
  implementation(project(":prime-modules"))

  // Match netty via ocs-grpc-api
  api("com.google.firebase:firebase-admin:${Version.firebase}")
}